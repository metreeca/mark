/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.pipes;

import com.metreeca.mark.Mark;
import com.metreeca.mark.Pipe;
import com.metreeca.mark.steps.Jade;
import com.metreeca.mark.steps.Markdown;

import java.nio.file.Path;

import static com.metreeca.mark.Mark.source;
import static com.metreeca.mark.Mark.target;


public final class Md implements Pipe {

	private final Markdown markdown;
	private final Jade jade;


	public Md(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

		this.markdown=new Markdown(mark);
		this.jade=new Jade(mark);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean process(final Path source, final Path target) {
		return source(source, ".md")
				.map(markdown::read)
				.map(model -> jade.write(target(target, ".html"), model))
				.isPresent();
	}

}
