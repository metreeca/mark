/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import com.metreeca.mark.processors.Verbatim;
import com.metreeca.mark.processors.Page;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Predicate.isEqual;


public final class Mark {

	private final Path source;
	private final Path target;
	private final Path layout;

	private final Log logger=new SystemStreamLog();

	private final List<Processor> processors;


	public Mark(final Path source, final Path target, final Path layout, final Map<String, Object> defaults) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		if ( !Files.exists(source) ) {
			throw new IllegalArgumentException("missing source folder {"+source+"}");
		}

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		if ( layout == null ) {
			throw new NullPointerException("null layout");
		}

		if ( defaults == null ) {
			throw new NullPointerException("null defaults");
		}

		this.source=source.toAbsolutePath().normalize();
		this.target=target.toAbsolutePath().normalize();

		if ( this.target.startsWith(this.source) || this.source.startsWith(this.target) ) {
			throw new IllegalArgumentException("overlapping source/target folders {"+source+" <-> "+target+"}");
		}

		this.layout=source.resolve(layout).toAbsolutePath();

		this.processors=asList(
				new Page(this.layout, defaults),
				new Verbatim(this.layout)
		);
	}


	public void process() {

		if ( Files.exists(target) ) {
			try (final Stream<Path> walk=Files.walk(target)) { // clean target folder

				walk.sorted(reverseOrder()).filter(isEqual(target).negate()).forEachOrdered(path -> {

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

		if ( Files.exists(source) ) {

			try (final Stream<Path> walk=Files.walk(source)) { // process source folder

				walk.sorted(Path::compareTo).filter(Files::isRegularFile).forEach(path -> {

					try {

						process(path);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e); // !!! report
					}

				});

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void process(final Path source) throws IOException {

		final Path target=this.target.resolve(this.source.relativize(source));

		Files.createDirectories(target.getParent());

		final String path=this.source.relativize(source).toString();

		try {

			processors.stream()
					.filter(processor -> processor.process(source, target))
					.findFirst()
					.ifPresent(processor -> logger.info(String.format("%-20s %s", processor, path)));

		} catch ( final RuntimeException e ) {

			logger.error(String.format("error while processing %s", path), e);

		}

	}

}
