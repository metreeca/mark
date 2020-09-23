/*
 * Copyright © 2019-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 *  file except in compliance with the License. You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.metreeca.mark.tasks;

import com.metreeca.mark.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.maven.plugin.logging.Log;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Stream;

import static com.metreeca.mark.Mark.*;
import static com.sun.net.httpserver.HttpServer.create;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.util.stream.Collectors.toMap;

/**
 * Site serving task.
 *
 * <p>Generates a processed version of the {@linkplain Opts#source() source} site folder in the
 * {@linkplain Opts#target() target} site folder and serves it on a development grade server for testing purposes,
 * watching the {@linkplain Opts#source() source} site folder for further changes to by synchronized.</p>
 */
public final class Serve implements Task {

	private static final int port=2020;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final int OK=200;
	private static final int NotFound=404;
	private static final int NotAllowed=405;

	private static final String Live=Optional

			.of(Serve.class.getSimpleName()+".js")

			.map(Serve.class::getResource)

			.map(url -> {
				try { return url.toURI(); } catch ( final URISyntaxException e ) { throw new RuntimeException(e); }
			})

			.map(Paths::get)

			.map(path -> {
				try {
					return new String(readAllBytes(path), UTF_8);
				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			})

			.map(script -> format("<script type=\"text/javascript\">\n\n%s\n\n</script>", script))

			.orElse("");


	/**
	 * File extension to MIME types.
	 *
	 * @see "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types"
	 */
	private static final Map<String, String> MIMEs=Stream

			.of(Serve.class.getSimpleName()+".tsv")

			.map(Serve.class::getResource)

			.map(url -> {
				try { return url.toURI(); } catch ( final URISyntaxException e ) { throw new RuntimeException(e); }
			})

			.map(Paths::get)

			.flatMap(path -> {
				try {
					return Files.readAllLines(path, UTF_8).stream();
				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			})

			.filter(line -> !line.isEmpty())

			.map(line -> {

				final int tab=line.indexOf('\t');

				return new AbstractMap.SimpleImmutableEntry<>(line.substring(0, tab), line.substring(tab+1));

			})

			.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final BlockingDeque<String> updates=new LinkedBlockingDeque<>();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void exec(final Mark mark) {
		serve(mark);
		watch(mark);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void serve(final Opts opts) {

		final Thread daemon=new Thread(() -> {

			try {

				final Path target=opts.target();
				final Log logger=opts.logger();

				final HttpServer server=create(new InetSocketAddress("localhost", port), 16);

				server.createContext("/", exchange -> new Thread(() -> handle(exchange, target)).start());

				server.start();

				final String home=format("http://%s:%d/",
						server.getAddress().getHostString(), server.getAddress().getPort()
				);

				logger.info(format("server listening at <%s>", home));

				open(home);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		});

		daemon.setDaemon(true);
		daemon.start();
	}

	private void watch(final Mark mark) {

		final Thread daemon=new Thread(() -> mark.watch(mark.target(), (kind, target) -> {

			final String path=Root.resolve(mark.target().relativize(target)).toString();

			Stream.of("", ".html", "index.html").forEach(suffix -> {
				if ( path.endsWith(suffix) ) { updates.offer(path.substring(0, path.length()-suffix.length())); }
			});

		}));

		daemon.setDaemon(true);
		daemon.start();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void handle(final HttpExchange exchange, final Path target) {
		try {

			final String method=exchange.getRequestMethod();

			if ( method.equals("GET") && exchange.getRequestURI().getPath().equals("/~") ) {

				try {

					final byte[] data=updates.take().getBytes(UTF_8);

					exchange.sendResponseHeaders(OK, data.length);

					try ( final OutputStream body=exchange.getResponseBody() ) { body.write(data); }

				} catch ( final InterruptedException e ) {

					exchange.sendResponseHeaders(OK, 0);

				}

			} else if ( method.equals("HEAD") || method.equals("GET") ) {

				final boolean head=exchange.getRequestMethod().equals("HEAD");

				final URI uri=exchange.getRequestURI(); // dot components already normalized

				final Path file=variants(uri.getPath())

						.map(Paths::get)
						.map(Root::relativize)
						.map(Path::normalize)
						.map(target::resolve)

						.filter(Files::exists)
						.filter(Files::isRegularFile)

						.findFirst()

						.orElse(null);

				if ( file != null ) {

					final String mime=MIMEs.getOrDefault(extension(file), "application/octet-stream");

					final byte[] data=readAllBytes(file);

					final byte[] body=mime.equals("text/html")
							? new String(data, UTF_8).replace("</body>", Live+"</body>").getBytes(UTF_8)
							: data;

					final Instant instant=Files
							.getLastModifiedTime(file)
							.toInstant();

					exchange.getResponseHeaders().set("Content-Type", mime);
					exchange.getResponseHeaders().set("ETag", format("\"%s\"", instant.toEpochMilli()));

					exchange.sendResponseHeaders(OK, head ? -1 : body.length);

					if ( !head ) {
						try ( final OutputStream output=exchange.getResponseBody() ) { output.write(body); }
					}

				} else {

					exchange.sendResponseHeaders(NotFound, head ? -1 : 0L);

				}

			} else {

				exchange.sendResponseHeaders(NotAllowed, 0L);

			}

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		} finally {

			exchange.close();

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void open(final String home) throws IOException {
		if ( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) ) {
			Desktop.getDesktop().browse(URI.create(home));
		}
	}

}
