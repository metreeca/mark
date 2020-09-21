/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.tasks;

import com.metreeca.mark.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.util.Comparator.reverseOrder;

/**
 * Site cleaning task.
 *
 * <p>Removes the {@linkplain Opts#target() target} site folder.</p>
 */
public final class Clean implements Task {

	@Override public void exec(final Mark mark) {

		final Path target=mark.target();

		if ( Files.exists(target) ) { // clean target folder

			try ( final Stream<Path> walk=Files.walk(target) ) {

				walk.sorted(reverseOrder()).forEachOrdered(path -> {

					try {

						Files.delete(path);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}

				});

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}
	}

}
