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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Predicate.isEqual;


public final class Mark {

	private static final Path Base=Paths.get("");


	public static String basename(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		final String name=path.getFileName().toString();
		final int dot=name.lastIndexOf('.');

		return dot >= 0 ? name.substring(0, dot) : name;

	}

	public static String extension(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		final String name=path.getFileName().toString();
		final int dot=name.lastIndexOf('.');

		return dot >= 0 ? name.substring(dot) : "";

	}


	private static Path normalize(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return path.toAbsolutePath().normalize();
	}

	private static Path resolve(final Path path, final Path child) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( child == null ) {
			throw new NullPointerException("null child");
		}

		return path.getFileSystem().equals(child.getFileSystem()) ? path.resolve(child) : child;
	}

	private static boolean contains(final Path path, final Path child) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( child == null ) {
			throw new NullPointerException("null child");
		}

		return path.getFileSystem().equals(child.getFileSystem()) && child.startsWith(path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Path source=Base;
	private Path target=Base;

	private Path assets=Base;
	private Path layout=Base;

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


	public Path assets() {
		return assets;
	}

	public Mark assets(final Path assets) {

		if ( assets == null ) {
			throw new NullPointerException("null assets");
		}

		this.assets=assets; // source-relative

		return this;
	}


	public Path layout() {
		return layout;
	}

	public Mark layout(final Path layout) {

		if ( layout == null ) {
			throw new NullPointerException("null layout");
		}

		this.layout=layout; // assets-relative

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

	/**
	 * Computes the relative base path of the site root.
	 *
	 * @param path the reference path
	 *
	 * @return the relative path of the root of the generated site wrt {@code path}
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public Path base(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return Optional.of(path.getParent().relativize(target))
				.filter(isEqual(Base).negate())
				.orElse(Paths.get("."));
	}

	/**
	 * Computes the relative path of a page.
	 *
	 * @param path the reference path
	 *
	 * @return the relative path of {@code path} wrt the root of the generated site
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public Path path(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return target().relativize(path);
	}


	public Path layout(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( name.isEmpty() || name.equals(extension(layout)) ) { // ;( loaders may force extension on empty paths…

			return layout;

		} else {

			final String base=relative(layout.getParent()).toString();

			for (final Path folder : asList(source, assets)) {

				final Path layout=folder.resolve(base).resolve(name);

				if ( exists(layout) ) {

					if ( !isRegularFile(layout) ) {
						throw new IllegalArgumentException("layout is not a regular file {"+layout+"}");
					}

					if ( !layout.startsWith(folder) ) {
						throw new IllegalArgumentException("layout outside base folder {" +folder+ " // "+layout+"}");
					}

					return layout;
				}

			}

			throw new IllegalArgumentException("unknown layout {"+name+"}");

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Mark build() {
		return exec(handler -> {

			if ( exists(target) ) { // clean target folder

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

			if ( !contains(source, assets) ) { // process skin assets

				try (final Stream<Path> walk=Files.walk(assets)) {

					final long start=currentTimeMillis();
					final long count=walk.filter(handler::apply).count();
					final long stop=currentTimeMillis();

					if ( count > 0 ) {
						logger.info(String.format("extracted %,d files in %,.3f s", count, (stop-start)/1000f));
					}

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}

			}

			if ( !contains(assets, source) ) { // process source folder

				try (final Stream<Path> walk=Files.walk(source)) {

					final long start=currentTimeMillis();
					final long count=walk.filter(handler::apply).count();
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
					sources.filter(Files::isDirectory).forEach(register); // register existing source folders
				}

				logger.info(String.format("watching %s", Base.toAbsolutePath().relativize(source)));

				for (WatchKey key; (key=service.take()) != null; key.reset()) { // watch source changes
					for (final WatchEvent<?> event : key.pollEvents()) {

						final Kind<?> kind=event.kind();
						final Path path=((Path)key.watchable()).resolve((Path)event.context());

						if ( event.kind().equals(ENTRY_CREATE) && isDirectory(path) ) { // register new folders

							logger.info(source.relativize(path).toString());

							register.accept(path);

						} else if ( event.kind().equals(ENTRY_CREATE) && isRegularFile(path) ) {

							handler.apply(path);

						} else if ( event.kind().equals(ENTRY_MODIFY) && isRegularFile(path) ) {

							if ( isLayout(path) ) { build(); } else { handler.apply(path); }

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

		this.source=normalize(source);
		this.target=normalize(target);

		if ( !exists(source) ) {
			throw new IllegalArgumentException("missing source folder {"+source+"}");
		}

		if ( !isDirectory(source) ) {
			throw new IllegalArgumentException("source is not a folder {"+source+"}");
		}

		if ( exists(target) && !isDirectory(target) ) {
			throw new IllegalArgumentException("target is not a folder {"+target+"}");
		}

		if ( contains(source, target) || contains(target, source) ) {
			throw new IllegalArgumentException("overlapping source/target folders {"+source+" // "+target+"}");
		}


		if ( assets.equals(Base) && layout.equals(Base) ) {

			this.assets=normalize(assets("/skins/docs"));
			this.layout=normalize(assets.resolve("assets/default.jade"));

		} else {

			this.assets=normalize(resolve(source, assets));
			this.layout=normalize(resolve(assets, layout));

		}

		if ( !exists(layout) ) {
			throw new IllegalArgumentException("missing default layout {"+layout+"}");
		}

		if ( !isRegularFile(layout) ) {
			throw new IllegalArgumentException("default layout is not a regular file {"+layout+"}");
		}

		if ( !contains(assets, layout) ) {
			throw new IllegalArgumentException("default layout outside assets folder {"+layout+"}");
		}

		if ( contains(target, assets) || contains(target, source) ) {
			throw new IllegalArgumentException("overlapping target/assets folders {"+target+" // "+assets+"}");
		}


		final Collection<Pipe> pipes=asList(
				new Md(this),
				new Wild()
		);

		task.accept(_source -> {

			if ( isDirectory(_source) || isLayout(_source) ) { return false; } else {

				try {

					final Path _common=relative(_source);
					final Path _target=target.resolve(_common.toString()); // possibly on different filesystems

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

			}

		});

		return this;
	}


	private Path assets(final String name) {

		final URL resource=getClass().getResource(name);

		if ( resource == null ) {
			throw new NullPointerException("unknown skin {" +name+ "}");
		}

		final String assets=resource.toString();

		if ( assets.startsWith("file:") ) {

			return normalize(Paths.get(assets.substring("file:".length())));

		} else if ( assets.startsWith("jar:") ) {

			final int mark=assets.indexOf('!');

			final String head=mark >= 0 ? assets.substring(0, mark) : assets;
			final String tail=mark >= 0 ? assets.substring(mark+1) : "/";

			try {

				return newFileSystem(URI.create(head), emptyMap()).getPath(tail);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		} else {

			throw new UnsupportedOperationException("unsupported assets scheme {"+assets+"}");

		}
	}


	private boolean isLayout(final Path path) {
		return extension(path).equals(extension(layout));
	}

	private Path relative(final Path path) {
		return contains(source, path) ? source.relativize(path)
				: contains(assets, path) ? assets.relativize(path)
				: path;
	}

}
