/*
 * Copyright © 2019-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 *  file except in compliance with the License. You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.metreeca.mark;

import com.metreeca.mark.pipes.*;

import com.sun.nio.file.SensitivityWatchEventModifier;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.file.FileSystems.newFileSystem;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isHidden;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


/**
 * Site generation engine.
 */
public final class Mark implements Opts {

	private static final Path root=Paths.get("/");
	private static final Path base=Paths.get("").toAbsolutePath();

	private static final Map<URI, FileSystem> bundles=new ConcurrentHashMap<>();

	private static final Pattern MessagePattern=Pattern.compile("\n\\s*");
	private static final Pattern URLPattern=Pattern.compile("(.*/)?(\\.|[^/#]*)?(#[^/#]*)?$");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Optional<Path> target(final Path source, final String to, final String... from) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		if ( to == null ) {
			throw new NullPointerException("null target extension");
		}

		if ( from == null || Arrays.stream(from).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null source extension");
		}

		final String extension=extension(source);

		return Arrays.stream(from).anyMatch(extension::equalsIgnoreCase)
				? Optional.of(source.resolveSibling(basename(source)+to))
				: Optional.empty();
	}


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

		return dot >= 0 ? name.substring(dot).toLowerCase(ROOT) : "";
	}


	public static Stream<String> variants(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		final Matcher matcher=URLPattern.matcher(path);

		if ( matcher.matches() ) {

			final String head=Optional.ofNullable(matcher.group(1)).orElse("");
			final String file=Optional.ofNullable(matcher.group(2)).orElse("");
			final String hash=Optional.ofNullable(matcher.group(3)).orElse("");

			return file.isEmpty() || file.equals(".") ? Stream.of(head+"index.html"+hash)
					: file.endsWith(".html") ? Stream.of(head+file+hash)
					: Stream.of(head+file+hash, head+file+".html"+hash);

		} else {

			return Stream.of(path);

		}

	}


	private static boolean contains(final Path path, final Path child) {
		return compatible(path, child) && child.startsWith(path);
	}

	private static boolean compatible(final Path x, final Path y) {
		return x.getFileSystem().equals(y.getFileSystem());
	}


	private static Path absolute(final Path path) {
		return path.toAbsolutePath().normalize();
	}

	private static Path relative(final Path path) {
		return compatible(base, path) ? base.relativize(path.toAbsolutePath()) : path;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Opts opts;

	private final Path source;
	private final Path target;

	private final Path assets;
	private final Path layout;

	private final Map<String, Object> shared;

	private final Log logger;


	private final String template; // template layout extension

	private final Collection<Function<Mark, Pipe>> pipes=asList(
			None::new, Md::new, Less::new, Any::new
	);

	private final Map<Path, Map<String, Object>> pages=new ConcurrentSkipListMap<>(comparing(Path::toString));


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

		this.assets=assets(requireNonNull(opts.assets(), "null opts assets path"));
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

		if ( !Files.exists(assets) ) {
			throw new IllegalArgumentException("missing assets folder { "+relative(assets)+" }");
		}

		if ( !isDirectory(assets) ) {
			throw new IllegalArgumentException("assets is not a folder { "+relative(assets)+" }");
		}


		if ( contains(source, target) || contains(target, source) ) {
			throw new IllegalArgumentException(
					"overlapping source/target folders { "+relative(source)+" <-> "+relative(target)+" }"
			);
		}

		if ( contains(source, assets) || contains(assets, source) ) {
			throw new IllegalArgumentException(
					"overlapping source/assets folders { "+relative(source)+" <-> "+relative(assets)+" }"
			);
		}

		if ( contains(target, assets) || contains(assets, target) ) {
			throw new IllegalArgumentException(
					"overlapping target/assets folders { "+relative(target)+" <-> "+relative(assets)+" }"
			);
		}


		this.shared=requireNonNull(opts.shared(), "null opts shared variables");
		this.logger=requireNonNull(opts.logger(), "null opts system logger");

		this.template=extension(layout);

		if ( template.isEmpty() ) {
			throw new IllegalArgumentException("extension-less layout { "+layout+" }");
		}

	}


	private Path layout(final Path path) {
		return root.resolve(path).normalize(); // root-relative layout path
	}

	private Path assets(final Path path) {

		final String name=path.toString();

		return name.equals("@") ? empty()
				: name.startsWith("@/") ? bundled(name)
				: absolute(path);

	}


	private Path empty() {
		try {

			final Path empty=absolute(Files.createTempDirectory(null));

			empty.toFile().deleteOnExit();

			return empty;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	private Path bundled(final String name) {

		final URL url=getClass().getClassLoader().getResource(name);

		if ( url == null ) {
			throw new NullPointerException("unknown theme {"+name+"}");
		}

		final String scheme=url.getProtocol();

		if ( scheme.equals("file") ) {

			return absolute(Paths.get(url.getPath()));

		} else if ( scheme.equals("jar") ) {

			final String path=url.toString();

			final int mark=path.indexOf('!');

			final String head=mark >= 0 ? path.substring(0, mark) : path;
			final String tail=mark >= 0 ? path.substring(mark+1) : "/";

			final FileSystem bundle=bundles.computeIfAbsent(URI.create(head), uri -> {
				try {

					return newFileSystem(uri, emptyMap());

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}

			});

			return bundle.getPath(tail);

		} else {

			throw new UnsupportedOperationException("unsupported assets scheme {"+name+"}");

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Path source() {
		return source;
	}

	@Override public Path target() {
		return target;
	}

	@Override public Path assets() {
		return assets;
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

		return extension(path).equals(template);
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

		final Path layout=source(root.relativize(
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
	 * Watches site folders.
	 *
	 * @param root   the root of the site folder to be monitored for changes
	 * @param action an action to be performed on change events; takes as argument the kind of change event and the
	 *               path of the changed file
	 *
	 * @return this engine
	 *
	 * @throws NullPointerException if either {@code root} or {@code action} is null
	 */
	public Mark watch(final Path root, final BiConsumer<WatchEvent.Kind<?>, Path> action) {

		if ( root == null ) {
			throw new NullPointerException("null root");
		}

		if ( action == null ) {
			throw new NullPointerException("null action");
		}

		final Thread thread=new Thread(() -> {

			try ( final WatchService service=root.getFileSystem().newWatchService() ) {

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

				try ( final Stream<Path> sources=Files.walk(root) ) {
					sources.filter(Files::isDirectory).forEach(register); // register existing folders
				}

				for (WatchKey key; (key=service.take()) != null; key.reset()) { // watch changes
					for (final WatchEvent<?> event : key.pollEvents()) {

						final WatchEvent.Kind<?> kind=event.kind();
						final Path path=((Path)key.watchable()).resolve((Path)event.context());

						if ( kind.equals(OVERFLOW) ) {

							logger.error("sync lost ;-(");

						} else if ( kind.equals(ENTRY_CREATE) && isDirectory(path) ) { // register new folders

							logger.info(root.relativize(path).toString());

							register.accept(path);

						} else if ( kind.equals(ENTRY_DELETE) || Files.isRegularFile(path) ) {

							action.accept(kind, path);

						}
					}
				}

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} catch ( final InterruptedException ignored ) {

				logger.error("interrupted…");

			}

		});

		thread.setDaemon(true);
		thread.start();

		return this;
	}


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

		logger.info(format("%s %s + %s ›› %s",
				task.getClass().getSimpleName().toLowerCase(ROOT),
				relative(source), relative(assets), relative(target)
		));

		task.exec(this);

		return this;
	}

	/**
	 * Processes site resources.
	 *
	 * <p>Generates processed version of site resources in the {@linkplain Opts#target() target} site folder.</p>
	 *
	 * @param paths the paths of the site resources to be processed, relative either to the {@linkplain Opts#source()
	 *              source} site folder or to the {@linkplain Opts#assets() assets} folder
	 *
	 * @return the number of processed resources
	 *
	 * @throws NullPointerException if {@code source} is null
	 */
	public long process(final Stream<Path> paths) {

		if ( paths == null ) {
			throw new NullPointerException("null path stream");
		}

		return paths

				// 1st pass: locate, compile and register pages

				.map(this::source)
				.map(this::compile)

				.filter(Optional::isPresent)
				.map(Optional::get)

				.map(this::register)

				.peek(page -> logger.info(page.path().toString()))

				// collect all page models before rendering

				.collect(toList())
				.stream()

				// 2nd pass >> render pages

				.peek(this::render)

				.count();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Page> compile(final Path source) {
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

							return Optional.<Page>empty();

						}
					})

					.filter(Optional::isPresent)
					.map(Optional::get)

					.findFirst();

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

	private Page register(final Page page) {

		final Path path=common(page.path()).normalize();
		final Map<String, Object> model=new HashMap<>(page.model());

		model.put("root", Optional
				.ofNullable(path.getParent())
				.map(parent -> root.resolve(parent).relativize(root)) // ;( must be both absolute
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

		if ( path.toString().endsWith(".html") ) {
			pages.put(path, model);
		}

		return new Page(path, model, page.render());
	}

	private void render(final Page page) {
		try {

			// create the root data model

			final Map<String, Object> model=new HashMap<>(shared());

			model.put("page", page.model());
			model.put("pages", pages.values());


			// make sure the output folder exists, then render page

			final Path target=target(page.path());

			Files.createDirectories(target.getParent());

			page.render().accept(target, model);


		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Path source(final Path path) {

		final Path absolute=path.isAbsolute() ? path : Stream.of(source, assets)

				.map(folder -> folder.resolve(path.toString())) // use strings to handle incompatible filesystems
				.map(Path::toAbsolutePath)
				.filter(Files::exists)

				.findFirst()
				.orElse(path);

		if ( Stream.of(source, assets).noneMatch(folder -> contains(folder, absolute)) ) {
			throw new IllegalArgumentException("resource outside input folders { "+relative(path)+" }");
		}

		return absolute.normalize();

	}

	private Path target(final Path path) {
		return target.resolve(path.toString()); // use strings to handle incompatible filesystems
	}

	private Path common(final Path path) {
		return Paths.get((

				contains(source, path) ? source.relativize(path)
						: contains(assets, path) ? assets.relativize(path)
						: path

		).toString()); // use strings to handle incompatible filesystems
	}

}
