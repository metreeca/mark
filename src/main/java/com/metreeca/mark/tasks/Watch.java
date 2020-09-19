/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.tasks;

import com.metreeca.mark.*;

import com.sun.nio.file.SensitivityWatchEventModifier;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Site watching task.
 *
 * <p>Generates a processed version of the {@linkplain Opts#source() source} site folder in the
 * {@linkplain Opts#target() target} site folder, watching the {@linkplain Opts#source() source} site folder for
 * further changes to by synchronized.</p>
 */
public final class Watch implements Task {

	@Override public void exec(final Mark mark) {

		final Path source=mark.source();
		final Path assets=mark.assets();

		final Log logger=mark.logger();

		try ( final WatchService service=source.getFileSystem().newWatchService() ) {

			final Consumer<Path> register=path -> {
				try {

					path.register(service,
							new WatchEvent.Kind<?>[]{ ENTRY_CREATE, ENTRY_MODIFY },
							SensitivityWatchEventModifier.HIGH
					);

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			};

			try ( final Stream<Path> sources=Files.walk(source) ) {
				sources.filter(Files::isDirectory).forEach(register); // register existing source folders
			}

			if ( source.getFileSystem().equals(assets.getFileSystem()) ) { // ignore bundled assets
				try ( final Stream<Path> assetses=Files.walk(assets) ) {
					assetses.filter(Files::isDirectory).forEach(register); // register existing assets folders
				}
			}

			for (WatchKey key; (key=service.take()) != null; key.reset()) { // watch source changes
				for (final WatchEvent<?> event : key.pollEvents()) {

					final WatchEvent.Kind<?> kind=event.kind();
					final Path path=((Path)key.watchable()).resolve((Path)event.context());

					if ( event.kind().equals(ENTRY_CREATE) && Files.isDirectory(path) ) { // register new folders

						logger.info(source.relativize(path).toString());

						register.accept(path);

					} else if ( event.kind().equals(ENTRY_CREATE) && Files.isRegularFile(path) ) {

						mark.process(path);

					} else if ( event.kind().equals(ENTRY_MODIFY) && Files.isRegularFile(path) ) {

						if ( mark.isLayout(path) ) { mark.exec(new Build()); } else { mark.process(path); }

					} else if ( kind.equals(OVERFLOW) ) {

						logger.error("sync lost ;-(");

					}
				}
			}

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		} catch ( final InterruptedException ignored ) {

			logger.error("interrupted…");

		}

	}

}
