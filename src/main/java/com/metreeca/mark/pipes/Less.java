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

import static com.inet.lib.less.Less.compile;
import static com.metreeca.mark.Mark.target;
import static java.nio.charset.StandardCharsets.UTF_8;


public final class Less implements Pipe {

	public Less(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Page> process(final Path source) {
		return target(source, ".css", ".less", ".css").map(target -> new Page(target) {

			@Override public void render(final Path target, final Map<String, Object> model) {
				try {

					final String less=new String(Files.readAllBytes(source), UTF_8);
					final String css=compile(target.toUri().toURL(), less, true);

					Files.write(target, css.getBytes(UTF_8));

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}

			}

		});
	}

}
