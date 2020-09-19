/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.mojos;

import com.metreeca.mark.Mark;
import com.metreeca.mark.tasks.Crawl;

import org.apache.maven.plugins.annotations.Mojo;


@Mojo(name="crawl") public class CrawlMojo extends MarkMojo {

	@Override public void execute() {
		new Mark(opts())
				.exec(new Crawl());
	}

}
