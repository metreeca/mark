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

import com.metreeca.mark.*;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * Site building task.
 *
 * <p>Generates a processed version of the {@linkplain Opts#source() source} site folder in the
 * {@linkplain Opts#target() target} site folder.</p>
 */
public final class Build implements Task {

    private static final String SiteURL="project.distributionManagement.site.url";

    private static final Pattern ExpressionPattern=Pattern.compile("(?<!\\\\)\\$\\{(\\w+(?:\\.\\w+)*)}");
    private static final Pattern RelativePattern=Pattern.compile("\\[([^]\\[]*)]\\((?!\\w+:)([^)]+)\\)");
    private static final Pattern EscapePattern=Pattern.compile("\\\\\\$");
    private static final Pattern DollarPattern=Pattern.compile("\\$");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public void exec(final Mark mark) {

        // copy index file to project folder

        mark.index().ifPresent(entry -> {

            try {

                final Path index=entry.getKey();
                final Path readme=entry.getValue();

                final String base=mark.global(String.class, SiteURL)
                        .orElseGet(() -> readme.getParent().relativize(mark.target()).toString());

                final String text=Optional.of(Files.readString(index, UTF_8))

                        // replace ${*} variables

                        .map(s -> ExpressionPattern.matcher(s).replaceAll(result ->
                                mark.global(String.class, result.group(1)).orElseGet(() ->
                                        DollarPattern.matcher(result.group()).replaceAll("\\\\\\$")
                                )
                        ))


                        // remove expression escapes

                        .map(s -> EscapePattern.matcher(s).replaceAll("\\$"))

                        // relocate relative links

                        .map(s -> RelativePattern.matcher(s).replaceAll(result -> format(

                                "[%s](%s)", result.group(1), base.startsWith("http")

                                        ? format(base.endsWith("/") ? "%s%s" : "%s/%s", base, result.group(2))
                                        .replace("/./", "/")

                                        .replace(".md", ".html")
                                        .replace("/index.html", "/")

                                        : Paths.get(base, result.group(2)).normalize())

                        ))

                        .orElseThrow();


                Files.write(readme, List.of(text), CREATE, TRUNCATE_EXISTING);

            } catch ( final IOException e ) {
                throw new UncheckedIOException(e);
            }

        });

        // copy bundled assets to source folder

        mark.assets().forEach((path, url) -> {

            final Path source=mark.source().resolve(path);

            try ( final InputStream resource=url.openStream() ) {

                Files.createDirectories(source.getParent());
                Files.copy(resource, source, REPLACE_EXISTING);

            } catch ( final IOException e ) {
                throw new UncheckedIOException(e);
            }

        });

        // process resources

        try ( final Stream<Path> sources=Files.walk(mark.source()) ) {

            final long start=currentTimeMillis();
            final long count=mark.process(sources).size();
            final long stop=currentTimeMillis();

            if ( count > 0 ) {
                mark.logger().info(format("processed ‹%,d› files in ‹%,.3f› s", count, (stop-start)/1000.0f));
            }

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }

}
