/*
 * Copyright © 2019-2023 Metreeca srl
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

import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isHidden;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Arrays.asList;
import static java.util.Locale.ROOT;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


/**
 * Site generation engine.
 */
public final class Mark implements Opts {

    private static final Path Root=Paths.get("/");
    private static final Path Base=Paths.get("");
    private static final Path Work=Base.toAbsolutePath().normalize();

    private static final String Date=ISO_LOCAL_DATE.format(LocalDate.now());

    private static final Pattern IndexPattern=Pattern.compile("^[^.]*");
    private static final Pattern MessagePattern=Pattern.compile("\n\\s*");


    //// Bundled Layout ////////////////////////////////////////////////////////////////////////////////////////////////

    private static final Path Layout=Paths.get("index.pug");

    private static final Map<Path, URL> Assets=Stream.of(

            "index.js", "index.less", "index.pug", "index.svg"

    ).collect(toMap(Paths::get, asset -> requireNonNull(

            Mark.class.getResource(format("files/%s", asset)),
            format("missing asset ‹%s›", asset)

    )));


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Opts opts;

    private final Path source;
    private final Path target;
    private final Path layout;

    private final Map<String, Object> global;

    private final boolean readme; // readme generation
    private final boolean inplace; // in-place processing
    private final boolean bundled; // use bundled skin

    private final Log logger;

    private final String template; // template layout extension


    private final Collection<Function<Mark, Pipe>> pipes=asList(
            None::new, Md::new, Less::new, Any::new
    );


    /**
     * Creates a site generation engine
     *
     * @param opts the site generation options
     *
     * @throws NullPointerException if {@code opts} is null or one of its methods returns a null value
     */
    public Mark(final Opts opts) {

        this.opts=requireNonNull(opts, "null opts");

        final Path _source=requireNonNull(opts.source(), "null source path");
        final Path _target=requireNonNull(opts.target(), "null target path");
        final Path _layout=requireNonNull(opts.layout(), "null layout path");

        this.source=_source.toAbsolutePath().normalize();
        this.target=_target.equals(Base) ? source : _target.toAbsolutePath().normalize();
        this.layout=_target.equals(Base) ? Layout : source.relativize(source.resolve(_layout)).normalize();

        this.readme=opts.readme();
        this.inplace=target.equals(source);
        this.bundled=layout.equals(Layout);

        if ( !Files.exists(source) ) {
            throw new IllegalArgumentException(format("missing source folder ‹%s›", local(source)));
        }

        if ( !isDirectory(source) ) {
            throw new IllegalArgumentException(format("source path ‹%s› is not a folder", local(source)));
        }

        if ( Files.exists(target) && !isDirectory(target) ) {
            throw new IllegalArgumentException(format("target path ‹%s› is not a folder", local(target)));
        }

        if ( !inplace && (target.startsWith(source) || source.startsWith(target)) ) {
            throw new IllegalArgumentException(
                    format("partly overlapping source/target folders ‹%s›/‹%s›", local(source), local(target))
            );
        }

        if ( !bundled && !Files.exists(source.resolve(layout)) ) {
            throw new IllegalArgumentException(format("missing layout ‹%s›", local(layout)));
        }

        if ( !bundled && !Files.isRegularFile(source.resolve(layout)) ) {
            throw new IllegalArgumentException(format("layout path ‹%s› is not a file", local(layout)));
        }


        this.global=Map.copyOf(requireNonNull(opts.global(), "null global variables"));
        this.logger=requireNonNull(opts.logger(), "null logger");


        final String _template=layout.toString();

        this.template=_template.substring(max(0, _template.lastIndexOf('.')));

        if ( !template.startsWith(".") ) {
            throw new IllegalArgumentException(format("layout ‹%s› has no extension", layout));
        }

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


    @Override public boolean readme() {
        return readme;
    }


    @Override public Map<String, Object> global() {
        return global;
    }

    @Override public <V> V option(final String option, final Function<String, V> mapper) {
        return opts.option(option, mapper);
    }


    @Override public Log logger() {
        return logger;
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

        return source(name.isEmpty() || name.equals(template) ? layout // ;( handle extension-only paths…
                : layout.resolveSibling(name.contains(".") ? name : name+template)
        );
    }


    public Optional<Path> target(final Path source, final String to, final String... from) {

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


    public Optional<Entry<Path, Path>> index() {
        if ( readme ) {

            final Path index=source.resolve("index.md").normalize();

            if ( !Files.exists(index) ) {
                logger.error(format("missing index file <%s>", relative(index)));
            }

            return Optional.of(entry(index, Work.resolve("README.md").normalize()));

        } else {

            return Optional.empty();

        }
    }

    public Map<Path, URL> assets() {
        return bundled ? Assets : Map.of();
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

        final String name=task.getClass().getSimpleName().toLowerCase(ROOT);

        logger.info(source.equals(target)
                ? format("%s %s", name, local(source))
                : format("%s %s ›› %s", name, local(source), local(target))
        );

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

                        path.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

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
     * @return the collection of generated site files
     *
     * @throws NullPointerException if {@code source} is null
     */
    public Collection<File> process(final Stream<Path> paths) {

        if ( paths == null ) {
            throw new NullPointerException("null path stream");
        }

        final Collection<File> files=scan(paths);

        final List<Map<String, Object>> models=files.stream()
                .filter(page -> page.path().toString().endsWith(".html"))
                .map(File::model)
                .collect(toList());

        files.forEach(file -> process(file, models));

        return files;
    }

    /**
     * Identifies site resources to be processed.
     *
     * @param paths the paths of the site resources to be processed, relative to the {@linkplain Opts#source() source}
     *              site folder
     *
     * @return the collection of site files to be generated
     *
     * @throws NullPointerException if {@code source} is null
     */
    public Collection<File> scan(final Stream<Path> paths) {

        if ( paths == null ) {
            throw new NullPointerException("null paths");
        }

        return paths

                .filter(Objects::nonNull)
                .filter(Files::isRegularFile)

                .map(this::source)
                .map(this::scan).flatMap(Optional::stream)
                .map(this::extend)

                .collect(toList());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Optional<File> scan(final Path source) {
        try {

            return isDirectory(source) || isHidden(source) || isLayout(source) ? Optional.empty() : pipes.stream()

                    .map(factory -> factory.apply(this))

                    .map(pipe -> {
                        try {

                            return pipe.process(source).filter(not(file ->
                                            inplace && file.path().equals(source) //
                                    // prevent overwriting
                            ));


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

        final Path relative=relative(file.path());

        final Map<String, Object> model=new HashMap<>(file.model());

        model.put("root", Optional
                .ofNullable(relative.getParent())
                .map(parent -> Root.resolve(parent).relativize(Root)) // ;( must be both absolute
                .map(Path::toString)
                .orElse(".")
        );

        model.put("base", Optional
                .ofNullable(relative.getParent())
                .map(Path::toString)
                .orElse(".")
        );

        model.put("path", relative.toString());

        model.putIfAbsent("date", Date);

        return new File(relative, model, file.process());
    }

    private void process(final File file, final List<Map<String, Object>> models) {
        try {

            final Path relative=relative(file.path());

            logger.info(relative.toString());


            // create the root data model

            final Map<String, Object> model=new HashMap<>(global);

            model.put("page", file.model());
            model.put("pages", models);


            // make sure the output folder exists, then process file

            final Path path=target(relative);

            Files.createDirectories(path.getParent());

            file.process().accept(path, model);

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Path relative(final Path path) {
        return source.relativize(source.resolve(path)).normalize();
    }

    private Path source(final Path path) {

        final Path absolute=source.resolve(path).toAbsolutePath().normalize();

        if ( !absolute.startsWith(source) ) {
            throw new IllegalArgumentException(format("resource ‹%s› outside source folder", local(path)));
        }

        if ( !Files.exists(absolute) ) {
            throw new IllegalArgumentException(format("missing resource ‹%s›", local(path)));
        }

        if ( !Files.isRegularFile(absolute) ) {
            throw new IllegalArgumentException(format("resource is not a regular file ‹%s›", local(path)));
        }

        return absolute;
    }

    private Path target(final Path path) {

        final Path absolute=target.resolve(path).toAbsolutePath().normalize();

        if ( !absolute.startsWith(target) ) {
            throw new IllegalArgumentException(format("resource ‹%s› outside target folder", local(path)));
        }

        if ( Files.exists(absolute) && !Files.isRegularFile(absolute) ) {
            throw new IllegalArgumentException(format("resource is not a regular file ‹%s›", local(path)));
        }

        return absolute;
    }


    private Path local(final Path path) {
        return Work.relativize(path.toAbsolutePath()).normalize();
    }

}
