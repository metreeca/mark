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

package com.metreeca.mark;

import com.metreeca.mark.pipes.*;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isHidden;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


/**
 * Site generation engine.
 */
public final class Mark {

	public static final Path Root=Paths.get("/");
	private static final Path Base=Paths.get("").toAbsolutePath();

	private static final Pattern MessagePattern=Pattern.compile("\n\\s*");


	public static Optional<Path> target(final Path source, final String to, final String... from) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		if ( to == null ) {
			throw new NullPointerException("null target extension");
		}

		if ( !to.startsWith(".") ) {
			throw new IllegalArgumentException("missing leading dot in target extension");
		}

		if ( from == null || Arrays.stream(from).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null source extension");
		}

		if ( Arrays.stream(from).anyMatch(ext -> !ext.startsWith(".")) ) {
			throw new IllegalArgumentException("missing leading dot in source extension");
		}


		final String path=source.toString();

		return Arrays.stream(from)

				.map(extension -> path.endsWith(extension)
						? source.resolveSibling(path.substring(0, path.length()-extension.length())+to)
						: null
				)

				.filter(Objects::nonNull)

				.findFirst();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Opts opts;

	private final String template; // template layout extension


	private final Collection<Function<Mark, Pipe>> pipes=asList(
			None::new, Md::new, Less::new, Any::new
	);

	private final Map<Path, Map<String, Object>> models=new ConcurrentSkipListMap<>(comparing(Path::toString));


	/**
	 * Creates a site generation engine
	 *
	 * @param opts the site generation options
	 *
	 * @throws NullPointerException if {@code opts} is null or one of its methods returns a null value
	 */
	public Mark(final Opts opts) {

		if ( opts == null ) {
			throw new NullPointerException("null opts");
		}


		final Path source=requireNonNull(opts.source(), "null source path");
		final Path target=requireNonNull(opts.source(), "null target path");
		final Path layout=requireNonNull(opts.source(), "null layout path");

		if ( !source.isAbsolute() ) {
			throw new IllegalArgumentException(format("relative source path ‹%s›", source));
		}

		if ( !source.equals(source.normalize()) ) {
			throw new IllegalArgumentException(format("denormalized source path ‹%s›", source));
		}

		if ( !Files.exists(source) ) {
			throw new IllegalArgumentException(format("missing source folder ‹%s›", relative(source)));
		}

		if ( !isDirectory(source) ) {
			throw new IllegalArgumentException(format("source path ‹%s› is not a folder", relative(source)));
		}


		if ( !target.isAbsolute() ) {
			throw new IllegalArgumentException(format("relative target path ‹%s›", target));
		}

		if ( !target.equals(target.normalize()) ) {
			throw new IllegalArgumentException(format("denormalized target path ‹%s›", target));
		}

		if ( Files.exists(target) && !isDirectory(target) ) {
			throw new IllegalArgumentException(format("target path ‹%s› is not a folder", relative(target)));
		}

		if ( !target.equals(source) && (target.startsWith(source) || source.startsWith(target)) ) {
			throw new IllegalArgumentException(
					format("overlapping source/target folders ‹%s›/‹%s›", relative(source), relative(target))
			);
		}


		final String path=layout.toString();
		final int dot=path.lastIndexOf('.');

		if ( dot < 0 ) {
			throw new IllegalArgumentException(format("layout ‹%s› has no extension", layout));
		}


		this.opts=opts;
		this.template=path.substring(dot);
	}


	public Opts opts() {
		return opts;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks if a path is a layout
	 *
	 * @param path the path to be checked
	 *
	 * @return {@code true} if {@code path} has the same file extension as the default {@linkplain Opts#layout() layout}
	 *
	 * @throws NullPointerException if {@code path} is {@code null}
	 */
	public boolean isLayout(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return path.toString().endsWith(template);
	}

	/**
	 * Locates a layout.
	 *
	 * @param name the name of the layout to be located
	 *
	 * @return the absolute path of the layout identified by {@code name}
	 *
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if unable to locate a layout identified by {@code name}
	 */
	public Path layout(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		// identify the absolute path of the layout ;(handling extension-only paths…)

		final Path layout=source(Root.relativize(
				name.isEmpty() || name.equals(template) ? opts.layout()
						: opts.layout().resolveSibling(name.contains(".") ? name : name+template).normalize()
		));

		if ( !Files.isRegularFile(layout) ) {
			throw new IllegalArgumentException("layout is not a regular file { "+relative(layout)+" }");
		}

		return layout;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a site generation task.
	 *
	 * @param task the site generation task to be executed
	 *
	 * @return this engine
	 *
	 * @throws NullPointerException if {@code resource} is null
	 */
	public Mark exec(final Task task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		opts.logger().info(format("%s %s  ›› %s",
				task.getClass().getSimpleName().toLowerCase(ROOT), relative(opts.source()), relative(opts.target())
		));

		task.exec(this);

		return this;
	}


	/**
	 * Watches site source folder.
	 *
	 * @param action an action to be performed on change events; takes as argument the kind of change event and the path
	 *               of the changed file
	 *
	 * @return this engine
	 *
	 * @throws NullPointerException if {@code action} is null
	 */
	public Mark watch(final BiConsumer<WatchEvent.Kind<?>, Path> action) {

		if ( action == null ) {
			throw new NullPointerException("null action");
		}

		final Thread thread=new Thread(() -> {

			try ( final WatchService service=opts.source().getFileSystem().newWatchService() ) {

				final Consumer<Path> register=path -> {
					try {

						path.register(service,
								new WatchEvent.Kind<?>[]{ ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE },
								SensitivityWatchEventModifier.HIGH
						);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				};

				try ( final Stream<Path> sources=Files.walk(opts.source()) ) {
					sources.filter(Files::isDirectory).forEach(register); // register existing folders
				}

				for (WatchKey key; (key=service.take()) != null; key.reset()) { // watch changes
					for (final WatchEvent<?> event : key.pollEvents()) {

						final WatchEvent.Kind<?> kind=event.kind();
						final Path path=((Path)key.watchable()).resolve((Path)event.context());

						if ( kind.equals(OVERFLOW) ) {

							opts.logger().error("sync lost ;-(");

						} else if ( kind.equals(ENTRY_CREATE) && isDirectory(path) ) { // register new folders

							opts.logger().info(opts.source().relativize(path).toString());

							register.accept(path);

						} else if ( kind.equals(ENTRY_DELETE) || Files.isRegularFile(path) ) {

							action.accept(kind, path);

						}
					}
				}

			} catch ( final UnsupportedOperationException ignored ) {

			} catch ( final InterruptedException e ) {

				opts.logger().error("interrupted…");

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}

		});

		thread.setDaemon(true);
		thread.start();

		return this;
	}

	/**
	 * Processes site resources.
	 *
	 * <p>Generates processed version of site resources in the {@linkplain Opts#target() target} site folder.</p>
	 *
	 * @param paths the paths of the site resources to be processed, relative to the {@linkplain Opts#source() source}
	 *              site folder
	 *
	 * @return the number of processed resources
	 *
	 * @throws NullPointerException if {@code source} is null
	 */
	public long process(final Stream<Path> paths) {

		if ( paths == null ) {
			throw new NullPointerException("null path stream");
		}

		// 1st pass › locate, compile and extend

		final List<File> files=paths

				.filter(Objects::nonNull)

				.map(this::source)
				.map(this::compile).flatMap(Optional::stream)
				.map(this::extend)

				.collect(toList());

		// 2nd pass › collect models

		files.stream()
				.filter(page -> page.path().toString().endsWith(".html"))
				.forEach(page -> models.put(page.path(), page.model()));

		// 3rd pass › render

		files.forEach(this::process);

		return files.size();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<File> compile(final Path source) {
		try {

			return isDirectory(source) || isHidden(source) || isLayout(source) ? Optional.empty() : pipes

					.stream()
					.map(factory -> factory.apply(this))

					.map(pipe -> {
						try {

							return pipe.process(source);

						} catch ( final RuntimeException e ) {

							opts.logger().error(format("%s › %s",
									pipe.getClass().getSimpleName().toLowerCase(ROOT),
									MessagePattern.matcher(e.getMessage()).replaceAll("; ")
							));

							return Optional.<File>empty();

						}
					})

					.flatMap(Optional::stream)

					.findFirst();

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

	private File extend(final File file) {

		final Path path=opts.source().relativize(file.path()).normalize();

		final Map<String, Object> model=new HashMap<>(file.model());

		model.put("root", Optional
				.ofNullable(path.getParent())
				.map(parent -> Root.resolve(parent).relativize(Root)) // ;( must be both absolute
				.map(Path::toString)
				.orElse(".")
		);

		model.put("base", Optional
				.ofNullable(path.getParent())
				.map(Path::toString)
				.orElse(".")
		);

		model.put("path", path.toString());

		model.computeIfAbsent("date", key -> ISO_LOCAL_DATE.format(LocalDate.now()));

		return new File(path, model, file.process());
	}

	private void process(final File file) {
		try {

			opts.logger().info(file.path().toString());

			// create the root data model

			final Map<String, Object> model=new HashMap<>(opts.global());

			model.put("page", file.model());
			model.put("pages", models.values());


			// make sure the output folder exists, then render file

			final Path target=target(file.path());

			Files.createDirectories(target.getParent());

			file.process().accept(target, model);

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Path source(final Path path) {

		// !!! Files::exists

		final Path absolute=opts.source()
				.resolve(path.toString()) // use strings to handle incompatible filesystems
				.toAbsolutePath()
				.normalize();

		if ( absolute.startsWith(opts.source()) ) {
			throw new IllegalArgumentException("resource outside input folders { "+relative(path)+" }");
		}

		return absolute.normalize();
	}

	private Path target(final Path path) {
		return opts.target()
				.resolve(path.toString()) // use strings to handle incompatible filesystems
				.toAbsolutePath()
				.normalize();
	}


	private Path relative(final Path path) {
		return Base.relativize(path.toAbsolutePath()).normalize();
	}

}
