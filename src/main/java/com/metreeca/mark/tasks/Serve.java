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
import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.mark.Mark.extension;
import static com.metreeca.mark.Mark.variants;
import static com.sun.net.httpserver.HttpServer.create;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
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

	private static final Path Root=Paths.get("/");

	private static final Pattern BodyPattern=Pattern.compile("</body>");
	private static final String LiveJS="<script type='text/javascript' src='https://livejs.com/live.js'></script>$0";


	/**
	 * File extension to MIME types.
	 *
	 * @see "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types"
	 */
	private static final Map<String, String> types=Stream

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

	@Override public void exec(final Mark mark) {

		final Thread daemon=new Thread(() -> {

			try {

				final Path target=mark.target();
				final Log logger=mark.logger();

				final HttpServer server=create(new InetSocketAddress("localhost", port), 0);

				server.createContext("/", exchange -> handle(exchange, target));

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void handle(final HttpExchange exchange, final Path target) throws IOException {
		try {

			final String method=exchange.getRequestMethod();

			if ( method.equals("HEAD") || method.equals("GET") ) {

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

					final String mime=types.getOrDefault(extension(file), "application/octet-stream");

					final byte[] data=readAllBytes(file);

					final byte[] body=mime.equals("text/html")
							? BodyPattern.matcher(new String(data, UTF_8)).replaceAll(LiveJS).getBytes(UTF_8)
							: data;

					final Instant instant=Files
							.getLastModifiedTime(file)
							.toInstant();

					exchange.getResponseHeaders().set("Content-Type", mime);
					exchange.getResponseHeaders().set("Last-Modified",
							instant.atOffset(UTC).format(RFC_1123_DATE_TIME));
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
