/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import java.nio.file.Path;


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
	 * @param target the path of the target file
	 *
	 * @return {@code true} if this pipe actually handled the {@code source} file; {@code false}, otherwise
	 */
	public boolean process(final Path source, final Path target);

}
