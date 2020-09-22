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

package com.metreeca.mark.pipes;

import com.metreeca.mark.*;
import com.metreeca.mark.steps.Markdown;
import com.metreeca.mark.steps.Pug;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static com.metreeca.mark.Mark.target;


public final class Md implements Pipe {

	// !!! remove


	private final Markdown markdown;
	private final Pug pug;


	public Md(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

		this.markdown=new Markdown(mark);
		this.pug=new Pug(mark);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Page> process(final Path source) {
		return target(source, ".html", ".md").map(target -> new Page(target, markdown.read(source)) {
			@Override public void render(final Path target, final Map<String, Object> model) {
				pug.write(target, model);
			}
		});
	}

}
