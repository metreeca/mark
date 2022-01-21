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

import org.apache.maven.plugin.logging.Log;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

/**
 * Site generation options.
 */
public interface Opts {

	/**
	 * @return the path of source site folder
	 */
	public Path source();

	/**
	 * @return the path of target site folder
	 */
	public Path target();

	/**
	 * @return the source-relative path of the default site layout
	 */
	public Path layout();


	/**
	 * @return the global variables
	 */
	public Map<String, Object> global();

	/**
	 * Retrieves a pipeline option.
	 *
	 * @param option the name of the option to be retrieved
	 * @param mapper a function mapping from a possibly null string value to an option value of the expected type
	 * @param <V>    the expected type of the option value
	 *
	 * @return the value produced by applying {@code mapper} to a possibly null string value retrieved from a
	 * system-specific source or {@code null} the optipn is not defined
	 *
	 * @throws NullPointerException if {@code mapper} is null
	 */
	public <V> V get(final String option, final Function<String, V> mapper);


	/**
	 * @return the system logger
	 */
	public Log logger();

}
