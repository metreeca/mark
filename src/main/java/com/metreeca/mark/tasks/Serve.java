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

import com.metreeca.jse.Server;
import com.metreeca.mark.*;
import com.metreeca.rest.Response;
import com.metreeca.rest.handlers.Publisher;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.metreeca.mark.Mark.Root;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Wrapper.postprocessor;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.TextFormat.text;
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

						.handler(context -> gateway().wrap(router()

								.path("/~", router().get(request -> request.reply(response -> {
									try {

										return response.status(OK).body(text(), updates.take());

									} catch ( final InterruptedException e ) {

										return response.status(OK);

									}
								})))

								.path("/*", router().get(Publisher.publisher(opts.target())
										.with(postprocessor(output(), this::rewrite))
								))

						))

						.start();

				final String home=format("http://%s:%d/", address.getHostString(), address.getPort());

				opts.logger().info(format("server listening at <%s>", home));

				if ( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) ) {
					Desktop.getDesktop().browse(URI.create(home));
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


	private Consumer<OutputStream> rewrite(final Response response, final Consumer<OutputStream> target) {
		if ( response.header("Content-Type").filter(mime -> mime.startsWith("text/html")).isPresent() ) {

			final ByteArrayOutputStream buffer=new ByteArrayOutputStream(1000);

			target.accept(buffer);

			final byte[] data=buffer.toByteArray();
			final Charset charset=Charset.forName(response.charset());

			final byte[] body=new String(data, charset)
					.replace("</body>", Live+"</body>")
					.getBytes(charset);

			return output -> {
				try {
					output.write(body);
				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			};

		} else {

			return target;

		}
	}

}
