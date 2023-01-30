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

package com.metreeca.mark.pipes;

import com.metreeca.mark.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static com.inet.lib.less.Less.compile;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Less/CSS processing pipe.
 *
 * <p>Converts {@code .less} and {@code .css} files under the {@code source} folder to minified {@code .css} files at
 * the same relative path under the {@code target} folder.</p>
 */
public final class Less implements Pipe {

    private final Mark mark;


    public Less(final Mark mark) {

        if ( mark == null ) {
            throw new NullPointerException("null mark");
        }

        this.mark=mark;
    }


    @Override public Optional<File> process(final Path source) {
        return mark.target(source, ".css", ".css", ".less").map(target ->
                new File(target, Map.of(), (path, model) -> {
                    try {

                        final String less=Files.readString(source);
                        final String css=compile(path.toUri().toURL(), less, true);

                        Files.write(path, css.getBytes(UTF_8));

                    } catch ( final IOException e ) {
                        throw new UncheckedIOException(e);
                    }
                })
        );
    }

}
