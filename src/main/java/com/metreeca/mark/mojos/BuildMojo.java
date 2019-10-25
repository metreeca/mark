/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.mojos;

import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name="build") public class BuildMojo extends MarkMojo {

	@Override public void execute() {
		mark().build();
	}

}
