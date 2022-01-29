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

package com.metreeca.mark.mojos;

import com.metreeca.mark.Opts;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;

public abstract class MarkMojo extends AbstractMojo implements Opts {

    @Parameter(defaultValue="${project}", readonly=true)
    private MavenProject project;


    @Parameter(defaultValue="${project.basedir}/docs/", property="mark.source")
    private String source;

    @Parameter(defaultValue="", property="mark.target")
    private String target;

    @Parameter(defaultValue="", property="mark.layout")
    private String layout;


    @Parameter(defaultValue="false", property="mark.readme")
    private boolean readme;


    @Parameter
    private Map<String, String> options;


    @Override public Path source() {
        return Paths.get(source == null ? "" : source);
    }

    @Override public Path target() {
        return Paths.get(target == null ? "" : target);
    }

    @Override public Path layout() { return Paths.get(layout == null ? "" : layout); }


    @Override public boolean readme() {
        return readme;
    }


    @Override public Map<String, Object> global() {
        return Map.of("project", project);
    }

    @Override public <V> V option(final String option, final Function<String, V> mapper) {
        return mapper.apply(options == null ? null : options.get(option));
    }


    @Override public Log logger() { return getLog(); }

}
