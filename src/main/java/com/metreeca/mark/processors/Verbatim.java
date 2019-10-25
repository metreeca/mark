/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.processors;

import com.metreeca.mark.Processor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.metreeca.mark.processors.Page.extension;


public final class Verbatim implements Processor {

	private final Path base;
	private final String type;


	public Verbatim(final Path layout) {

		if ( layout == null ) {
			throw new NullPointerException("null layout");
		}

		this.base=layout.getParent();
		this.type=extension(layout);
	}


	@Override public boolean process(final Path source, final Path target) {
		if ( !source.startsWith(base) || !source.toString().endsWith(type) ) { // ignore layouts

			try {

				Files.copy(source, target);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

			return true;

		} else {

			return false;

		}
	}


	@Override public String toString() {
		return "verbatim";
	}

}
