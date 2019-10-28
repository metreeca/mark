/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import java.nio.file.Path;
import java.util.function.Consumer;


public interface Pipe extends Task<Path, Consumer<Path>> {}
