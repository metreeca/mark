/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.pipes;

import com.metreeca.mark.Pipe;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.Math.max;


public final class Wild implements Pipe {

	private final Path base;
	private final String type;


	public Wild(final Path layout) {

		if ( layout == null ) {
			throw new NullPointerException("null layout");
		}

		this.base=layout.getParent();
		this.type=type(layout);
	}


	@Override public String process(final Path source, final Path target) {
		if ( source.startsWith(base) && type(source).equals(type) ) { return ""; } else { // ignore layouts

			try {

				Files.copy(source, target);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

			return "verbatim";

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static String type(final Path path) {
		return type(path.toString());
	}

	private static String type(final String path) {
		return path.substring(max(0, path.lastIndexOf('.')));
	}

}
