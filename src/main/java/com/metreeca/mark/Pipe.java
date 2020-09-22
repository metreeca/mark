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
