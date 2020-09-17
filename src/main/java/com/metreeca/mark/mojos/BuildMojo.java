/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.mojos;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name="build", defaultPhase=LifecyclePhase.SITE) public class BuildMojo extends MarkMojo {

	@Override public void execute() {
		mark().build();
	}

}
