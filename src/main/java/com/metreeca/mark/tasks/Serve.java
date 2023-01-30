/*
 * Copyright © 2019-2023 Metreeca srl
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

import com.metreeca.core.services.Logger;
import com.metreeca.core.toolkits.Resources;
import com.metreeca.http.Request;
import com.metreeca.http.Response;
import com.metreeca.http.codecs.Data;
import com.metreeca.http.handlers.*;
import com.metreeca.jse.JSEServer;
import com.metreeca.json.codecs.JSON;
import com.metreeca.mark.*;

import org.apache.maven.plugin.logging.Log;

import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.*;

import javax.json.Json;

import static com.metreeca.core.services.Logger.logger;
import static com.metreeca.http.Handler.handler;
import static com.metreeca.http.Response.OK;

import static java.lang.String.format;

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

    private static final String ReloadTag=format("<script type=\"text/javascript\" src=\"%s\"></script>", ReloadScript);


    public static void main(final String... args) {
        new Serve().exec(new Mark(new Opts() {

            @Override public Path source() {
                return Paths.get("docs");
            }

            @Override public Path target() {
                return Paths.get("");
            }

            @Override public Path layout() {
                return Paths.get("");
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

        }));
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

                                .get(() -> new Router()

                                        .path(ReloadQueue, new Worker()
                                                .get(this::queue)
                                        )

                                        .path(ReloadScript, new Worker()
                                                .get(this::script)
                                        )

                                        .path("/*", new Worker()
                                                .get(handler(

                                                        this::rewrite,

                                                        new Publisher()
                                                                .assets(mark.target())

                                                ))
                                        )
                                )
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

    private Response queue(final Request request, final Function<Request, Response> forward) {
        try {

            final Collection<String> batch=new HashSet<>(Set.of(updates.take()));

            updates.drainTo(batch);

            return request.reply(OK)
                    .body(new JSON(), Json.createArrayBuilder(batch).build());

        } catch ( final InterruptedException e ) {

            return request.reply(OK);

        }
    }

    private Response script(final Request request, final Function<Request, Response> forward) {
        return request.reply(OK)
                .header("Content-Type", "text/javascript")
                .body(new Data(), Resources.data(Serve.class, ".js"));
    }


    private Response rewrite(final Request request, final Function<Request, Response> forward) {
        return forward.apply(request).map(response -> {
            if ( response.header("Content-Type").filter(mime -> mime.startsWith("text/html")).isPresent() ) {

                final ByteArrayOutputStream buffer=new ByteArrayOutputStream(1000);

                response.output().accept(buffer);

                final byte[] body=buffer.toString(response.charset())
                        .replace("</head>", format("%s</head>", ReloadTag))
                        .getBytes(response.charset());

                return response
                        .header("Content-Length", String.valueOf(body.length))
                        .output(output -> {
                            try {
                                output.write(body);
                            } catch ( final IOException e ) {
                                throw new UncheckedIOException(e);
                            }
                        });

            } else {

                return response;

            }

        });
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
