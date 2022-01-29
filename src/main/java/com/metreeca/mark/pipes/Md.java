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

package com.metreeca.mark.pipes;

import com.metreeca.mark.*;
import com.metreeca.mark.steps.Markdown;
import com.metreeca.mark.steps.Pug;

import java.nio.file.Path;
import java.util.Optional;


/**
 * Markdown processing pipe.
 *
 * <p>Converts {@code .md} files under the {@code source} folder to .html files at the same relative path under the
 * {@code target} folder, using the default Pug template specified by the {@code layout} parameter or by the layout
 * front-matter property; links to other {@code .md} files are converted to the corresponding {@code .html} file.</p>
 */
public final class Md implements Pipe {

    private final Mark mark;

    private final Markdown markdown;
    private final Pug pug;


    public Md(final Mark mark) {

        if ( mark == null ) {
            throw new NullPointerException("null mark");
        }

        this.mark=mark;

        this.markdown=new Markdown(mark);
        this.pug=new Pug(mark);
    }


    @Override public Optional<File> process(final Path source) {
        return mark.target(source, ".html", ".md").map(target ->
                new File(target, markdown.read(source), pug::write)
        );
    }

}
