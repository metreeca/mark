/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.mojos;

import com.metreeca.mark.Mark;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Paths;

import static java.util.Collections.singletonMap;


@Mojo(name="build") public class BuildMojo extends AbstractMojo {

	@Parameter( defaultValue = "${project}", readonly = true, required = true )
	private MavenProject project;


	@Parameter( property = "build.source", defaultValue = "${project.basedir}/src/docs/" )
	private String source;

	@Parameter( property = "build.target", defaultValue = "${project.build.directory}/docs/" )
	private String target;

	@Parameter( property = "build.layout", defaultValue = "" )
	private String layout;



	public void execute() {
		new Mark()

				.source(Paths.get(source))
				.target(Paths.get(target))
				.layout(Paths.get(layout))

				.shared(singletonMap("project", project))
				.logger(getLog())

				.build();
	}

}
