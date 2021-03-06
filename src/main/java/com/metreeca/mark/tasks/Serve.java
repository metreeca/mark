/*
 * Copyright © 2019-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.mark.tasks;

import com.metreeca.jse.Server;
import com.metreeca.mark.*;
import com.metreeca.rest.Response;
import com.metreeca.rest.assets.Logger;

import org.apache.maven.plugin.logging.Log;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.*;
import java.util.stream.Stream;

import static com.metreeca.mark.Mark.Root;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Wrapper.postprocessor;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.rest.handlers.Publisher.publisher;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.wrappers.Gateway.gateway;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;

/**
 * Site serving task.
 *
 * <p>Generates a processed version of the {@linkplain Opts#source() source} site folder in the
 * {@linkplain Opts#target() target} site folder and serves it on a development grade server for testing purposes,
 * watching the {@linkplain Opts#source() source} site folder for further changes to by synchronized.</p>
 */
public final class Serve implements Task {

	private final InetSocketAddress address=new InetSocketAddress("localhost", 2020);

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

				new Server()

						.address(address)

						.handler(context -> context

								.set(logger(), () -> new MavenLogger(opts.logger()))

								.get(() -> gateway().wrap(router()

										.path("/~", router()
												.get(request -> request.reply(response -> {
													try {

														return response.status(OK).body(text(), updates.take());

													} catch ( final InterruptedException e ) {

														return response.status(OK);

													}
												}))
										)

										.path("/*", router()
												.get(publisher(opts.target())
														.with(postprocessor(this::rewrite))
												)
										)

								))
						)

						.start();

				if ( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) ) {
					Desktop.getDesktop().browse(URI.create(format(
							"http://%s:%d/", address.getHostString(), address.getPort()
					)));
				}


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


	private Response rewrite(final Response response) {
		if ( response.header("Content-Type").filter(mime -> mime.startsWith("text/html")).isPresent() ) {

			return response.body(output()).fold(e -> response, target -> {

				final ByteArrayOutputStream buffer=new ByteArrayOutputStream(1000);

				target.accept(buffer);

				final byte[] data=buffer.toByteArray();
				final Charset charset=Charset.forName(response.charset());

				final byte[] body=new String(data, charset)
						.replace("</body>", Live+"</body>")
						.getBytes(charset);

				return response
						.header("Content-Length", String.valueOf(body.length))
						.body(output(), output -> {
							try {
								output.write(body);
							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}
						});

			});

		} else {

			return response;

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class MavenLogger extends Logger {

		private final Log logger;


		private MavenLogger(final Log logger) {
			this.logger=logger;
		}


		@Override public Logger entry(
				final Level level, final Object source,
				final Supplier<String> message, final Throwable cause
		) {

			if ( cause == null ) {

				final Consumer<String> sink=(level == Level.Error) ? logger::error
						: (level == Level.Warning) ? logger::warn
						: (level == Level.Info) ? logger::info
						: logger::debug;

				sink.accept(message.get());


			} else {

				final BiConsumer<String, Throwable> sink=(level == Level.Error) ? logger::error
						: (level == Level.Warning) ? logger::warn
						: (level == Level.Info) ? logger::info
						: logger::debug;

				sink.accept(message.get(), cause);

			}

			return this;
		}
	}

}
