/*
 * Copyright © 2019-2022 Metreeca srl
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
				final Stream<Path> sources=Files.walk(opts.source())
		) {

			final long start=currentTimeMillis();
			final long count=mark.process(sources);
			final long stop=currentTimeMillis();

			if ( count > 0 ) {
				opts.logger().info(String.format("processed %,d files in %,.3f s", count, (stop-start)/1000.0f));
			}

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

}
