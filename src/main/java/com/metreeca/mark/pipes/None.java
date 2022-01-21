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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class None implements Pipe {

	private static String basename(final Path path) { // !!!

		final String filename=path.toString();
		final int dot=filename.lastIndexOf('.');

		return dot < 0 ? filename : filename.substring(0, dot);
	}


	public None(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

	}


	@Override public Optional<File> process(final Path source) {
		return Optional.of(source)
				.filter(path -> !Files.exists(path))
				.map(path -> new File(path, Map.of(), (target, model) -> {
					try ( final Stream<Path> files=Files.walk(target.getParent()) ) {

						files.filter(file -> basename(path).equals(basename(file))).forEach(file -> {
							try {

								Files.delete(file);

							} catch ( final IOException e ) {

								throw new UncheckedIOException(e);

							}
						});

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}
				}));
	}

}
