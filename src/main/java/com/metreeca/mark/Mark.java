/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import com.metreeca.mark.processors.Page;
import com.metreeca.mark.processors.Verbatim;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Predicate.isEqual;


public final class Mark {

	private Path source=Paths.get("");
	private Path target=Paths.get("");
	private Path layout=Paths.get("");

	private Map<String, Object> values=emptyMap();
	private Log logger=new SystemStreamLog();


	public Mark source(final Path source) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		this.source=source.toAbsolutePath().normalize();

		return this;
	}

	public Mark target(final Path target) {

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		this.target=target.toAbsolutePath().normalize();

		return this;
	}

	public Mark layout(final Path layout) {

		if ( layout == null ) {
			throw new NullPointerException("null layout");
		}

		this.layout=layout; // source-relative

		return this;
	}


	public Mark shared(final Map<String, Object> shared) {

		if ( shared == null ) {
			throw new NullPointerException("null shared model");
		}

		this.values=unmodifiableMap(shared);

		return this;
	}

	public Mark logger(final Log logger) {

		if ( logger == null ) {
			throw new NullPointerException("null logger");
		}

		this.logger=logger;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Mark build() {

		if ( !Files.exists(source) ) {
			throw new IllegalArgumentException("missing source folder {"+source+"}");
		}

		if ( target.startsWith(source) || source.startsWith(target) ) {
			throw new IllegalArgumentException("overlapping source/target folders {"+source+" <-> "+target+"}");
		}

		// !!! handle empty layout

		final Path layout=source.resolve(this.layout);

		return clean().build(processor(asList(
				new Page(target, layout, values),
				new Verbatim(layout)
		)));
	}

	public void watch() {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Processor processor(final Collection<Processor> processors) {
		return (source, target) -> {

			final String path=this.source.relativize(source).toString();

			return processors.stream()
					.map(processor -> processor.process(source, target))
					.filter(status -> !status.isEmpty())
					.peek(status -> logger.info(String.format("%-25s %s", status, path)))
					.findFirst()
					.orElse("");

		};
	}


	private Mark clean() {

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

		return this;
	}

	private Mark build(final Processor processor) {

		if ( Files.exists(source) ) {

			try (final Stream<Path> walk=Files.walk(source)) { // process source folder

				final long start=currentTimeMillis();

				final long count=walk.sorted(Path::compareTo).filter(Files::isRegularFile).map(path -> {

					try {

						final Path target=this.target.resolve(source.relativize(path));

						Files.createDirectories(target.getParent());

						return processor.process(path, target);

					} catch ( final IOException|RuntimeException e ) {

						logger.error(String.format("error while processing %s", path), e);

						return "";

					}

				}).filter(status -> !status.isEmpty()).count();

				final long stop=currentTimeMillis();

				logger.info(String.format("processed %,d files in %,.3f s", count, (stop-start)/1000f));


			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}

		return this;
	}

}
