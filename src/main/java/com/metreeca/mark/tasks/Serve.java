/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
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
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.sun.net.httpserver.HttpServer.create;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;

/**
 * Site serving task.
 *
 * <p>Generates a processed version of the {@linkplain Opts#source() source} site folder in the
 * {@linkplain Opts#target() target} site folder and serves it on a development grade server for testing purposes,
 * watching the {@linkplain Opts#source() source} site folder for further changes to by synchronized.</p>
 */
public final class Serve implements Task {

	public static final int port=2020;


	/**
	 * File extension to MIME types.
	 *
	 * @see "https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types"
	 */
	private static final Map<String, String> types=Stream

			.of(Serve.class.getSimpleName())

			.map(Serve.class::getResource)

			.map(url -> {
				try { return url.toURI();} catch ( final URISyntaxException e ) { throw new RuntimeException(e); }
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

				final int colon=line.indexOf(':');

				return new AbstractMap.SimpleImmutableEntry<>(line.substring(0, colon), line.substring(colon+1));
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

			final URI uri=exchange.getRequestURI(); // dot components already normalized

			final Path path=Paths.get(uri.getPath()).normalize();
			final Path file=target.resolve(Paths.get("/").relativize(path));

			if ( method.equals("HEAD") || method.equals("GET") ) {

				if ( Files.isDirectory(file) ) {

					get(exchange, file.resolve("index.html"));

				} else if ( Files.isRegularFile(file) ) {

					get(exchange, file);

				} else {

					exchange.sendResponseHeaders(400, 0L);

				}

			} else {

				exchange.sendResponseHeaders(405, 0L);

			}

		} finally {

			exchange.close();

		}
	}

	private void get(final HttpExchange exchange, final Path file) throws IOException {

		final String mime=types.getOrDefault(Mark.extension(file), "application/octet-stream");

		final byte[] data=Files.readAllBytes(file);

		exchange.getResponseHeaders().set("Content-Type", mime);
		exchange.sendResponseHeaders(200, data.length);

		if ( exchange.getRequestMethod().equals("GET") ) {
			try ( final OutputStream output=exchange.getResponseBody() ) {
				output.write(data);
			}
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void open(final String home) throws IOException {
		if ( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) ) {
			Desktop.getDesktop().browse(URI.create(home));
		}
	}

}
