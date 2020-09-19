/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.mojos;

import com.metreeca.mark.Mark;
import com.metreeca.mark.tasks.Build;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name="build", defaultPhase=LifecyclePhase.SITE) public class BuildMojo extends MarkMojo {

	@Override public void execute() {
		new Mark(opts()).exec(new Build());
	}

}
