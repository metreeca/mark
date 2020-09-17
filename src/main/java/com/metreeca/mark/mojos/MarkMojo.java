/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.mojos;

import com.metreeca.mark.Mark;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Paths;

import static java.util.Collections.singletonMap;


public abstract class MarkMojo extends AbstractMojo {

	@Parameter(defaultValue="${project}", readonly=true)
	private MavenProject project;


	@Parameter(defaultValue="${project.basedir}/src/docs/", property="mark.source")
	private String source;

	@Parameter(defaultValue="${project.build.directory}/docs/", property="mark.target")
	private String target;

	@Parameter(defaultValue="", property="mark.assets")
	private String assets;

	@Parameter(defaultValue="", property="mark.layout")
	private String layout;


	/**
	 * Creates a mark processor.
	 *
	 * @return a new mark processor initialized from the exposed maven configuration properties
	 */
	protected Mark mark() {
		return new Mark()

				.source(Paths.get(source))
				.target(Paths.get(target))

				.assets(Paths.get(assets == null ? "" : assets)) // ;( maven ignores empty default values…
				.layout(Paths.get(layout == null ? "" : layout)) // ;( maven ignores empty default values…

				.shared(singletonMap("project", project))

				.logger(getLog());
	}

}
