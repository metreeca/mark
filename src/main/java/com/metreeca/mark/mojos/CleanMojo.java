/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.mojos;

import com.metreeca.mark.Mark;
import com.metreeca.mark.tasks.Clean;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name="clean", defaultPhase=LifecyclePhase.PRE_CLEAN) public class CleanMojo extends MarkMojo {

	@Override public void execute() {
		new Mark(opts()).exec(new Clean());
	}

}
