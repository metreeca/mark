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
import org.apache.maven.plugin.logging.Log;

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
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


/**
 * Site generation engine.
 */
public final class Mark implements Opts {

	public static final Path Root=Paths.get("/");
	private static final Path Base=Paths.get("").toAbsolutePath();

	private static final Pattern MessagePattern=Pattern.compile("\n\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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


	private static boolean contains(final Path path, final Path child) {
		return child.startsWith(path);
	}


	private static Path absolute(final Path path) {
		return path.toAbsolutePath().normalize();
	}

	private static Path relative(final Path path) {
		return Base.relativize(path.toAbsolutePath()).normalize();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Opts opts;

	private final Path source;
	private final Path target;
	private final Path layout;

	private final Map<String, Object> shared;

	private final Log logger;


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

		this.opts=opts;

		this.source=absolute(requireNonNull(opts.source(), "null opts source path"));
		this.target=absolute(requireNonNull(opts.target(), "null opts target path"));
		this.layout=layout(requireNonNull(opts.layout(), "null opts layout path"));


		if ( !Files.exists(source) ) {
			throw new IllegalArgumentException("missing source folder { "+relative(source)+" }");
		}

		if ( !isDirectory(source) ) {
			throw new IllegalArgumentException("source is not a folder { "+relative(source)+" }");
		}

		if ( Files.exists(target) && !isDirectory(target) ) {
			throw new IllegalArgumentException("target is not a folder { "+relative(target)+" }");
		}

		if ( contains(source, target) || contains(target, source) ) {
			throw new IllegalArgumentException(
					"overlapping source/target folders { "+relative(source)+" <-> "+relative(target)+" }"
			);
		}


		this.shared=requireNonNull(opts.shared(), "null opts shared variables");
		this.logger=requireNonNull(opts.logger(), "null opts system logger");


		final String path=layout.toString();
		final int dot=path.lastIndexOf('.');

		if ( dot < 0 ) {
			throw new IllegalArgumentException("extension-less layout { "+layout+" }");
		}

		this.template=path.substring(dot);
	}


	private Path layout(final Path path) {
		return Root.resolve(path).normalize(); // root-relative layout path
	}



	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Path source() {
		return source;
	}

	@Override public Path target() {
		return target;
	}

	@Override public Path layout() {
		return layout;
	}


	@Override public Map<String, Object> shared() {
		return unmodifiableMap(shared);
	}

	@Override public Log logger() {
		return logger;
	}


	@Override public <V> V get(final String option, final Function<String, V> mapper) {
		return opts.get(option, mapper);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks if a path is a layout
	 *
	 * @param path the path to be checked
	 *
	 * @return {@code true} if {@code path} has the same file extension as the default {@linkplain #layout() layout}
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
				name.isEmpty() || name.equals(template) ? this.layout
						: this.layout.resolveSibling(name.contains(".") ? name : name+template).normalize()
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

		logger.info(format("%s %s  ›› %s",
				task.getClass().getSimpleName().toLowerCase(ROOT), relative(source), relative(target)
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

			try ( final WatchService service=source.getFileSystem().newWatchService() ) {

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

				try ( final Stream<Path> sources=Files.walk(source) ) {
					sources.filter(Files::isDirectory).forEach(register); // register existing folders
				}

				for (WatchKey key; (key=service.take()) != null; key.reset()) { // watch changes
					for (final WatchEvent<?> event : key.pollEvents()) {

						final WatchEvent.Kind<?> kind=event.kind();
						final Path path=((Path)key.watchable()).resolve((Path)event.context());

						if ( kind.equals(OVERFLOW) ) {

							logger.error("sync lost ;-(");

						} else if ( kind.equals(ENTRY_CREATE) && isDirectory(path) ) { // register new folders

							logger.info(source.relativize(path).toString());

							register.accept(path);

						} else if ( kind.equals(ENTRY_DELETE) || Files.isRegularFile(path) ) {

							action.accept(kind, path);

						}
					}
				}

			} catch ( final UnsupportedOperationException ignored ) {

			} catch ( final InterruptedException e ) {

				logger.error("interrupted…");

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

		files.forEach(this::render);

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

							logger.error(format("%s › %s",
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

		final Path path=Paths.get((source.relativize(file.path())).toString()).normalize(); // use strings to handle
		// incompatible filesystems
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

		return new File(path, model, file.render());
	}

	private void render(final File file) {
		try {

			logger.info(file.path().toString());

			// create the root data model

			final Map<String, Object> model=new HashMap<>(shared());

			model.put("page", file.model());
			model.put("pages", models.values());


			// make sure the output folder exists, then render page

			final Path target=target(file.path());

			Files.createDirectories(target.getParent());

			file.render().accept(target, model);

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Path source(final Path path) {

		// !!! Files::exists

		final Path absolute=source
				.resolve(path.toString()) // use strings to handle incompatible filesystems
				.toAbsolutePath()
				.normalize();

		if ( contains(source, absolute) ) {
			throw new IllegalArgumentException("resource outside input folders { "+relative(path)+" }");
		}

		return absolute.normalize();
	}

	private Path target(final Path path) {
		return target
				.resolve(path.toString()) // use strings to handle incompatible filesystems
				.toAbsolutePath()
				.normalize();
	}

}
