/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import java.nio.file.Path;


@FunctionalInterface public interface Pipe {

	public boolean process(final Path source, final Path target);

}