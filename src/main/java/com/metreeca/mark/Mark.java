/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import com.metreeca.mark.tasks.Md;
import com.metreeca.mark.tasks.Wild;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Predicate.isEqual;


public final class Mark {

	private Path source=Paths.get("");
	private Path target=Paths.get("");

	private Path assets=Paths.get(""); // !!!
	private Path layout=Paths.get("");

	private Map<String, Object> shared=emptyMap();

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


	public Mark assets(final Path assets) {

		if ( assets == null ) {
			throw new NullPointerException("null assets");
		}

		this.assets=assets; // source-relative

		return this;
	}

	public Mark layout(final Path layout) {

		if ( layout == null ) {
			throw new NullPointerException("null layout");
		}

		this.layout=layout; // assets-relative

		return this;
	}


	public Mark shared(final Map<String, Object> shared) {

		if ( shared == null ) {
			throw new NullPointerException("null shared model");
		}

		this.shared=unmodifiableMap(shared);

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
		return exec((resources, handler) -> {

			clean();

			//  !!! merge resources

			build(handler);

		});
	}

	public void watch() {
		exec((resources, handler) -> {

			try {

				final WatchService service=source.getFileSystem().newWatchService();
				final Kind<?>[] events={ENTRY_CREATE, ENTRY_MODIFY};

				// !!! watch resources?

				Files.walk(source).filter(Files::isDirectory).forEach(path -> { // register existing folders
					try {

						path.register(service, events);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				});

				logger.info(String.format("watching %s", Paths.get("").toAbsolutePath().relativize(source)));

				for (WatchKey key; (key=service.take()) != null; key.reset()) { // watch changes
					for (final WatchEvent<?> event : key.pollEvents()) {

						final Kind<?> kind=event.kind();
						final Path path=((Path)key.watchable()).resolve((Path)event.context());

						if ( event.kind().equals(ENTRY_CREATE) && Files.isDirectory(path) ) {

							logger.info(source.relativize(path).toString());

							path.register(service, events); // register new folders

						} else if ( event.kind().equals(ENTRY_CREATE) && Files.isRegularFile(path) ) {

							handler.apply(path);

						} else if ( event.kind().equals(ENTRY_MODIFY) && Files.isRegularFile(path) ) {

							handler.apply(path);

						} else if ( kind.equals(OVERFLOW) ) {

							logger.error("sync lost");

						}
					}
				}

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} catch ( final InterruptedException ignored ) {}

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Mark exec(final BiConsumer<Stream<Path>, Function<Path, Boolean>> task) {

		if ( !Files.exists(source) ) {
			throw new IllegalArgumentException("missing source folder {"+source+"}");
		}

		if ( !Files.isDirectory(source) ) {
			throw new IllegalArgumentException("source is not a folder {"+source+"}");
		}

		if ( Files.exists(target) && !Files.isDirectory(target) ) {
			throw new IllegalArgumentException("target is not a folder {"+target+"}");
		}

		if ( target.startsWith(source) || source.startsWith(target) ) {
			throw new IllegalArgumentException("overlapping source/target folders {"+source+" <-> "+target+"}");
		}

		final Path assets=source; // !!! handle empty layout
		final Path layout=assets.resolve(this.layout);

		if ( !Files.exists(layout) ) {
			throw new IllegalArgumentException("missing default layout {"+layout+"}");
		}

		if ( !Files.isRegularFile(layout) ) {
			throw new IllegalArgumentException("layout is not a plain file {"+layout+"}");
		}

		if ( !layout.startsWith(source) ) {
			throw new IllegalArgumentException("default layout outside assets folder {"+layout+"}");
		}

		final Stream<Path> resources=Stream.empty(); // !!!

		final Function<Path, Boolean> handler=handler(asList(
				new Md(target, layout, shared),
				new Wild(layout)
		));

		task.accept(resources, handler);

		return this;
	}

	private Function<Path, Boolean> handler(final Collection<Task> pipes) {
		return source -> {

			try {

				final Path xxx=this.source.relativize(source);
				final Path target=this.target.resolve(xxx);

				Files.createDirectories(target.getParent());

				return pipes.stream()
						.filter(task -> task.process(source, target))
						.peek(status -> logger.info(xxx.toString()))
						.findFirst()
						.isPresent();

			} catch ( final IOException|RuntimeException e ) {

				logger.error(String.format("error while processing %s", source), e);

				return false;

			}

		};
	}


	private void clean() { // clean target folder
		if ( Files.exists(target) ) {
			try (final Stream<Path> walk=Files.walk(target)) {

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
	}

	private void build(final Function<Path, Boolean> handler) { // process source folder
		if ( Files.exists(source) ) {

			try (final Stream<Path> walk=Files.walk(source)) {

				final long start=currentTimeMillis();

				final long count=walk
						.filter(Files::isRegularFile)
						.filter(handler::apply)
						.count();

				final long stop=currentTimeMillis();

				logger.info(String.format("processed %,d files in %,.3f s", count, (stop-start)/1000f));

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}
	}

}
