/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import java.nio.file.Path;
import java.util.Optional;


/**
 * Site resource processing pipe.
 *
 * <p>Transfers processed resources from the source to the target folder.</p>
 */
@FunctionalInterface public interface Pipe {

	/**
	 * Processes a resource.
	 *
	 * @param source the path of the source file
	 *
	 * @return an optional site page, if this pipe actually handled the {@code source} file; an empty optional,
	 * otherwise
	 */
	public Optional<Page> process(final Path source);

}
