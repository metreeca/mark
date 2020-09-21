/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
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
import static java.util.Collections.emptyMap;


public final class Wild implements Pipe {

	public Wild(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Page> process(final Path source) {
		return Optional.of(new Page(source, emptyMap()) {
			@Override public void render(final Path target, final Map<String, Object> model) {
				try {

					Files.copy(source, target, REPLACE_EXISTING);

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			}
		});
	}

}
