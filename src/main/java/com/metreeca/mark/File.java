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

package com.metreeca.mark;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.util.Collections.unmodifiableMap;

/**
 * Site file.
 */
public final class File {

	private final Path path;
	private final Map<String, Object> model;
	private final BiConsumer<Path, Map<String, Object>> render;


	/**
	 * @param path
	 * @param model  may be extended
	 * @param render
	 */
	public File(final Path path, final Map<String, Object> model, final BiConsumer<Path, Map<String, Object>> render) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		if ( render == null ) {
			throw new NullPointerException("null render");
		}

		this.path=path;
		this.model=unmodifiableMap(model);
		this.render=render;
	}


	public Path path() {
		return path;
	}

	public Map<String, Object> model() {
		return model;
	}

	public BiConsumer<Path, Map<String, Object>> render() {
		return render;
	}

}
