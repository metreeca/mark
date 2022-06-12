/*
 * Copyright © 2019-2022 Metreeca srl
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

import com.metreeca.jse.JSEServer;
import com.metreeca.mark.*;
import com.metreeca.rest.*;
import com.metreeca.rest.services.Logger;

import org.apache.maven.plugin.logging.Log;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.*;

import javax.json.Json;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Wrapper.postprocessor;
import static com.metreeca.rest.formats.DataFormat.data;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.rest.handlers.Publisher.publisher;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.rest.wrappers.Server.server;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Site serving task.
 *
 * <p>Generates a processed version of the {@linkplain Opts#source() source} site folder in the
 * {@linkplain Opts#target() target} site folder and serves it on a development grade server for testing purposes,
 * watching the {@linkplain Opts#source() source} site folder for further changes to by synchronized.</p>
 */
public final class Serve implements Task {

    private static final String ReloadQueue="/~";
    private static final String ReloadScript="/~script";

    private static final String ReloadSrc=Serve.class.getSimpleName()+".js";
    private static final String ReloadTag=format("<script type=\"text/javascript\" src=\"%s\"></script>", ReloadScript);


    public static void main(final String... args) {
        new Serve().serve(new Opts() {

            @Override public Path source() {
                return Paths.get("docs");
            }

            @Override public Path target() {
                return Paths.get("docs");
            }

            @Override public Path layout() {
                return null;
            }

            @Override public boolean readme() {
                return false;
            }

            @Override public Map<String, Object> global() {
                return Map.of();
            }

            @Override public <V> V option(final String option, final Function<String, V> mapper) {
                return null;
            }

            @Override public Log logger() {
                return null;
            }

        });
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final InetSocketAddress address=new InetSocketAddress("localhost", 2020);

    private final BlockingDeque<String> updates=new LinkedBlockingDeque<>();


    @Override public void exec(final Mark mark) {
        watch(mark);
        serve(mark);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void watch(final Mark mark) {

        final Path root=Paths.get("/");

        final Thread daemon=new Thread(() -> mark.watch((kind, target) -> updates.offer(root

                .resolve(mark.target().relativize(target))
                .toString()
                .replace("/index.html", "/")

        )));

        daemon.setDaemon(true);
        daemon.start();
    }

    private void serve(final Opts mark) {

        final Thread daemon=new Thread(() -> {
            try {

                new JSEServer()

                        .address(address)

                        .delegate(context -> context

                                .set(logger(), () -> new MavenLogger(mark.logger()))

                                .get(() -> server().wrap(router()

                                        .path(ReloadQueue, router()
                                                .get(this::queue)
                                        )

                                        .path(ReloadScript, router()
                                                .get(this::script)
                                        )

                                        .path("/*", router()
                                                .get(publisher(mark.target())
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


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private com.metreeca.rest.Future<Response> queue(final Request request) {
        try {

            final Collection<String> batch=new HashSet<>(Set.of(updates.take()));

            updates.drainTo(batch);

            return request.reply(response -> response.status(OK)
                    .header("Content-Type", "application/json")
                    .body(text(), Json.createArrayBuilder(batch).build().toString())
            );

        } catch ( final InterruptedException e ) {

            return request.reply(status(OK));

        }
    }

    private Future<Response> script(final Request request) {
        try ( final InputStream script=requireNonNull(getClass().getResourceAsStream(ReloadSrc)) ) {

            return request.reply(response -> {
                try {
                    return response.status(OK)
                            .header("Content-Type", "text/javascript")
                            .body(data(), script.readAllBytes());
                } catch ( final IOException e ) {

                    throw new UncheckedIOException(e);

                }
            });

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }


    private Response rewrite(final Response response) {
        if ( response.header("Content-Type").filter(mime -> mime.startsWith("text/html")).isPresent() ) {

            return response.body(output()).fold(e -> response, target -> {

                final ByteArrayOutputStream buffer=new ByteArrayOutputStream(1000);

                target.accept(buffer);

                final Charset charset=Charset.forName(response.charset());

                final byte[] body=buffer.toString(charset)
                        .replace("</head>", format("%s</head>", ReloadTag))
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

                final Consumer<String> sink=(level == Level.error) ? logger::error
                        : (level == Level.warning) ? logger::warn
                        : (level == Level.info) ? logger::info
                        : logger::debug;

                sink.accept(message.get());


            } else {

                final BiConsumer<String, Throwable> sink=(level == Level.error) ? logger::error
                        : (level == Level.warning) ? logger::warn
                        : (level == Level.info) ? logger::info
                        : logger::debug;

                sink.accept(message.get(), cause);

            }

            return this;
        }
    }

}
