/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.mojos;

import com.metreeca.mark.Mark;
import com.metreeca.mark.tasks.*;

import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name="serve") public final class ServeMojo extends MarkMojo {

	@Override public void execute() {
		new Mark(opts())
				.exec(new Build())
				.exec(new Serve())
				.exec(new Watch());
	}

}
