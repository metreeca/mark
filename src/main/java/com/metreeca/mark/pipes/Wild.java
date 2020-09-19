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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


public final class Wild implements Pipe {

	public Wild(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean process(final Path source, final Path target) {
		try {

			Files.copy(source, target, REPLACE_EXISTING);

			return true;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

}
