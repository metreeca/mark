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
import java.util.Optional;
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
     * @return flag indicating whether to generate a README.md file in the project base folder from {@link #source()}
     * source}/index.md}
     */
    public boolean readme();


    /**
     * @return the global variables
     */
    public Map<String, Object> global();

    /**
     * @param path the dotted path of a global variable
     * @param <V>  the expected value type
     *
     * @return the optional value of the global variable identified by {@code path} or an empty optional is no matching
     * value is found
     *
     * @throws NullPointerException if either {@code type} or {@code path} is null
     */
    public default <V> Optional<V> global(final Class<V> type, final String path) {

        if ( type == null ) {
            throw new NullPointerException("null type");
        }

        if ( path == null ) {
            throw new NullPointerException("null path");
        }

        final String[] steps=path.split("\\.");

        if ( steps.length == 0 ) { return Optional.empty(); } else {

            Map<?, ?> map=global();

            for (int i=0; i < steps.length-1; i++) {

                final String step=steps[i];

                map=Optional.ofNullable(map.get(step))
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .orElseGet(Map::of);

            }

            return Optional.ofNullable(map.get(steps[steps.length-1]))
                    .filter(type::isInstance)
                    .map(type::cast);

        }
    }


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
    public <V> V option(final String option, final Function<String, V> mapper);


    /**
     * @return the system logger
     */
    public Log logger();

}
