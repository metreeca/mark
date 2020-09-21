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

import static java.lang.System.currentTimeMillis;

/**
 * Site building task.
 *
 * <p>Generates a processed version of the {@linkplain Opts#source() source} site folder in the
 * {@linkplain Opts#target() target} site folder.</p>
 */
public final class Build implements Task {

	@Override public void exec(final Mark mark) {
		try (
				final Stream<Path> assets=Files.walk(mark.assets());
				final Stream<Path> sources=Files.walk(mark.source())
		) {

			final long start=currentTimeMillis();
			final long count=mark.process(Stream.concat(assets, sources));
			final long stop=currentTimeMillis();

			if ( count > 0 ) {
				mark.logger().info(String.format("processed %,d files in %,.3f s", count, (stop-start)/1000.0f));
			}

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

}
