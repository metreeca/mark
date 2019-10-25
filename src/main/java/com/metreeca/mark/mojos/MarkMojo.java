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


	@Parameter(defaultValue="${project.basedir}/src/docs/")
	private String source;

	@Parameter(defaultValue="${project.build.directory}/docs/")
	private String target;


	@Parameter(defaultValue="")
	private String assets;

	@Parameter(required=true)
	private String layout;


	protected Mark mark() {
		return new Mark()

				.source(Paths.get(source))
				.target(Paths.get(target))

				.assets(Paths.get(assets == null? "" : assets)) // ;( maven ignores empty default values…
				.layout(Paths.get(layout == null? "" : layout)) // ;( maven ignores empty default values…

				.shared(singletonMap("project", project))

				.logger(getLog());
	}

}
