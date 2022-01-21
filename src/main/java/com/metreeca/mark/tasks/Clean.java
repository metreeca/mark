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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import static java.util.Comparator.reverseOrder;

/**
 * Site cleaning task.
 *
 * <p>Removes the {@linkplain Opts#target() target} site folder.</p>
 */
public final class Clean implements Task {

	@Override public void exec(final Mark mark) {

		final Path source=mark.source();
		final Path target=mark.target();

		if ( source.equals(target) ) { // in-place generation › remove assets and generated files

			try ( final Stream<Path> sources=Files.walk(mark.source()) ) {

				mark.scan(sources).forEach(file -> delete(target.resolve(file.path())));

				mark.assets().forEach((path, url) -> {

					final Path asset=target.resolve(path);

					try (
							final InputStream origin=url.openStream();
							final InputStream actual=Files.newInputStream(asset)
					) {

						if ( checksum(origin) == checksum(actual) ) { delete(asset); }

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}

				});

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		} else if ( Files.exists(target) ) { // delete target folder

			try ( final Stream<Path> walk=Files.walk(target) ) {

				walk.sorted(reverseOrder()).forEachOrdered(this::delete);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}
	}


	private static long checksum(final InputStream input) throws IOException {
		try ( final CheckedInputStream checker=new CheckedInputStream(input, new CRC32()) ) {

			final byte[] buffer=new byte[1024];

			while ( checker.read(buffer, 0, buffer.length) >= 0 ) { }

			return checker.getChecksum().getValue();
		}
	}


	private void delete(final Path path) {
		try {

			Files.delete(path);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

}
