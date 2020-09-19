/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.pipes;

import com.metreeca.mark.Mark;
import com.metreeca.mark.Pipe;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.inet.lib.less.Less.compile;
import static com.metreeca.mark.Mark.source;
import static com.metreeca.mark.Mark.target;
import static java.nio.charset.StandardCharsets.UTF_8;


public final class Less implements Pipe {

	public Less(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean process(final Path source, final Path target) {
		return source(source, ".less").map(path -> {

			try {

				final String less=new String(Files.readAllBytes(path), UTF_8);
				final String css=compile(path.toUri().toURL(), less, true);


				return Files.write(target(target, ".css"), css.getBytes(UTF_8));

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}).isPresent();
	}

}
