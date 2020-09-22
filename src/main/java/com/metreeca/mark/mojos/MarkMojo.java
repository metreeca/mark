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

import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;


public abstract class MarkMojo extends AbstractMojo {

	@Parameter(defaultValue="${project}", readonly=true)
	private MavenProject project;


	@Parameter(defaultValue="${project.basedir}/src/docs/", property="mark.source")
	private String source;

	@Parameter(defaultValue="${project.build.directory}/docs/", property="mark.target")
	private String target;

	@Parameter(defaultValue="@", property="mark.assets")
	private String assets;

	@Parameter(defaultValue="layouts/default.pug", property="mark.layout")
	private String layout;


	@Parameter
	private Map<String, String> options;


	/**
	 * Retrieves site generation options.
	 *
	 * @return site generation options initialized from the exposed maven configuration properties
	 */
	protected Opts opts() {
		return new MojoOpts(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class MojoOpts implements Opts {

		private final MarkMojo mojo;


		private MojoOpts(final MarkMojo mojo) {
			this.mojo=mojo;
		}


		@Override public Path source() { return Paths.get(mojo.source); }

		@Override public Path target() { return Paths.get(mojo.target); }

		@Override public Path assets() { return Paths.get(mojo.assets); }

		@Override public Path layout() { return Paths.get(mojo.layout); }


		@Override public Map<String, Object> shared() {
			return unmodifiableMap(singletonMap("project", mojo.project));
		}

		@Override public Log logger() { return mojo.getLog(); }


		@Override public <V> V get(final String option, final Function<String, V> mapper) {
			return mapper.apply(mojo.options == null ? null : mojo.options.get(option));
		}

	}

}
