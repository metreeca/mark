/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.mojos;

import com.metreeca.mark.Mark;
import com.metreeca.mark.tasks.Build;
import com.metreeca.mark.tasks.Watch;

import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name="watch") public class WatchMojo extends MarkMojo {

	@Override public void execute() {
		new Mark(opts()).exec(new Build()).exec(new Watch());
	}

}
