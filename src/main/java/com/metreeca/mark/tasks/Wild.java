/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.tasks;

import com.metreeca.mark.Mark;
import com.metreeca.mark.Task;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


public final class Wild implements Task {

	private final Path layout;


	public Wild(final Path layout) {

		if ( layout == null ) {
			throw new NullPointerException("null layout");
		}

		this.layout=layout;
	}


	@Override public boolean process(final Path source, final Path target) {
		if ( Mark.layout(source, layout) ) { return false; } else {
			try {

				Files.copy(source, target, REPLACE_EXISTING);

				return true;

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}
	}

}
