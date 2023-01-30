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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


/**
 * Generic processing pipe.
 *
 * <p>Transfers generic files under the {@code source} folder to verbatim copies at the same relative path under the
 * {@code target} folder.</p>
 */
public final class Any implements Pipe {

	public Any(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

	}


	@Override public Optional<File> process(final Path source) {
		return Optional.of(new File(source, Map.of(), (target, model) -> {
			try {

				Files.copy(source, target, REPLACE_EXISTING);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}));
	}

}
