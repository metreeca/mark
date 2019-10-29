/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import com.metreeca.mark.pipes.Md;
import com.metreeca.mark.pipes.Wild;

import com.sun.nio.file.SensitivityWatchEventModifier;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Predicate.isEqual;


public final class Mark {

	private static final Pattern ExtensionPattern=Pattern.compile("\\.[^.]+$");


	private Path skin() {
		try {

			final String assets="skins/default.zip";
			final String layout="assets/default.jade";

			final URL resource=Mark.class.getResource(assets);

			if ( resource == null ) {
				logger.error("missing assets archive {"+assets+"}");
			}

			final Path path=newFileSystem(URI.create("jar:"+resource), emptyMap()).getPath(layout);

			if ( !Files.exists(path) ) {
				logger.error("missing default assets layout {"+assets+" / "+path+"}");
			}

			return path;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	public Path base(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return path.getParent().equals(target)
				? Paths.get(".")
				: path.getParent().relativize(target);
	}

	public Path resolve(final String name) {
		if ( name.isEmpty() || name.equals(extension(layout)) ) { // extension may be forced on empty path by loaders

			return layout;

		} else if ( isSource(layout) ) {

			return layout.getParent().resolve(name);

		} else {

			final Path base=Paths.get(layout.getRoot().relativize(layout.getParent()).toString());
			final Path path=source.resolve(base).resolve(name);

			if ( Files.exists(path) ) {

				if ( !Files.isRegularFile(path) ) {
					throw new IllegalArgumentException("layout is not a regular file {"+path+"}");
				}

				if ( !path.startsWith(source) ) {
					throw new IllegalArgumentException("layout outside source folder {"+path+"}");
				}

				return path;

			} else {

				return layout.getParent().resolve(name);

			}

		}
	}


	private boolean isSource(final Path path) {
		return path.getFileSystem().equals(source.getFileSystem());
	}

	private boolean isLayout(final Path path) {
		return extension(path).equals(extension(layout))
				// !!! && path.startsWith(layout.getParent())
				;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Path target(final Path path, final String extension) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( extension == null ) {
			throw new NullPointerException("null extension");
		}

		return path.getParent().resolve(
				ExtensionPattern.matcher(path.getFileName().toString()).replaceFirst(extension)
		);
	}


	private static String extension(final Path path) {
		return extension(path.toString());
	}

	private static String extension(final String path) {
		return path.substring(max(0, path.lastIndexOf('.')));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Path source=Paths.get("");
	private Path target=Paths.get("");
	private Path layout=Paths.get("");

	private Map<String, Object> shared=emptyMap();

	private Log logger=new SystemStreamLog();


	public Path source() {
		return source;
	}

	public Mark source(final Path source) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		this.source=source; // cwd-relative

		return this;
	}


	public Path target() {
		return target;
	}

	public Mark target(final Path target) {

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		this.target=target; // cwd-relative

		return this;
	}


	public Path layout() {
		return layout;
	}

	public Mark layout(final Path layout) {

		if ( layout == null ) {
			throw new NullPointerException("null layout");
		}

		this.layout=layout; // source-relative

		return this;
	}


	public Map<String, Object> shared() {
		return unmodifiableMap(shared);
	}

	public Mark shared(final Map<String, Object> shared) {

		if ( shared == null ) {
			throw new NullPointerException("null shared model");
		}

		this.shared=unmodifiableMap(shared);

		return this;
	}


	public Log logger() {
		return logger;
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
		return exec(handler -> {

			if ( Files.exists(target) ) { // clean target folder

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

			if ( !isSource(layout) ) { // process skin assets

				try (final Stream<Path> walk=Files.walk(layout.getRoot())) {

					final long start=currentTimeMillis();

					final long count=walk // !!! factor
							.filter(Files::isRegularFile)
							.filter(path -> !isLayout(path))
							.filter(handler::apply)
							.count();

					final long stop=currentTimeMillis();

					if ( count > 0 ) {
						logger.info(String.format("extracted %,d files in %,.3f s", count, (stop-start)/1000f));
					}

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}

			}

			if ( Files.exists(source) ) { // process source folder

				try (final Stream<Path> walk=Files.walk(source)) {

					final long start=currentTimeMillis();

					final long count=walk // !!! factor
							.filter(Files::isRegularFile)
							.filter(path -> !isLayout(path))
							.filter(handler::apply)
							.count();

					final long stop=currentTimeMillis();

					if ( count > 0 ) {
						logger.info(String.format("processed %,d files in %,.3f s", count, (stop-start)/1000f));
					}

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}

			}

		});
	}

	public void watch() {
		exec(handler -> {

			try (final WatchService service=source.getFileSystem().newWatchService()) {

				final Consumer<Path> register=path -> {
					try {

						path.register(service,
								new Kind<?>[] {ENTRY_CREATE, ENTRY_MODIFY},
								SensitivityWatchEventModifier.HIGH
						);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				};

				try (final Stream<Path> sources=Files.walk(source)) {
					sources.filter(Files::isDirectory).forEach(register); // register existing folders
				}

				logger.info(String.format("watching %s", Paths.get("").toAbsolutePath().relativize(source)));

				for (WatchKey key; (key=service.take()) != null; key.reset()) { // watch changes
					for (final WatchEvent<?> event : key.pollEvents()) {

						final Kind<?> kind=event.kind();
						final Path path=((Path)key.watchable()).resolve((Path)event.context());

						if ( event.kind().equals(ENTRY_CREATE) && Files.isDirectory(path) ) { // register new folders

							logger.info(source.relativize(path).toString());

							register.accept(path);

						} else if ( event.kind().equals(ENTRY_CREATE) && Files.isRegularFile(path) ) {

							if ( !isLayout(path) ) {
								handler.apply(path);
							}

						} else if ( event.kind().equals(ENTRY_MODIFY) && Files.isRegularFile(path) ) {

							if ( isLayout(path) ) {

								build();

							} else {

								handler.apply(path);

							}

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

	private Mark exec(final Consumer<Function<Path, Boolean>> task) {

		this.source=source.toAbsolutePath().normalize();
		this.target=target.toAbsolutePath().normalize();

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

		if ( layout.equals(Paths.get("")) ) {

			this.layout=skin().toAbsolutePath().normalize();

		} else {

			this.layout=source.resolve(layout).toAbsolutePath().normalize();

			if ( !Files.exists(layout) ) {
				throw new IllegalArgumentException("missing default layout {"+layout+"}");
			}

			if ( !Files.isRegularFile(layout) ) {
				throw new IllegalArgumentException("default layout is not a plain file {"+layout+"}");
			}

			if ( !layout.startsWith(source) ) {
				throw new IllegalArgumentException("default layout outside source folder {"+layout+"}");
			}

		}

		final Collection<Pipe> pipes=asList(
				new Md(this),
				new Wild()
		);

		task.accept(_source -> {

			try {

				final Path _common=isSource(_source) ? source.relativize(_source)
						: Paths.get(_source.getRoot().relativize(_source).toString());

				final Path _target=target.resolve(_common);

				Files.createDirectories(_target.getParent());

				return pipes.stream()
						.filter(pipe -> pipe.process(_source, _target))
						.peek(status -> logger.info(_common.toString()))
						.findFirst()
						.isPresent();

			} catch ( final IOException|RuntimeException e ) {

				logger.error(String.format("error while processing %s", _source), e);

				return false;

			}

		});

		return this;
	}

}
