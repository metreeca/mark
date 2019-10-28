/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import java.nio.file.Paths;


final class MarkTest {

	public static void main(final String... args) {
		new Mark()

				.source(Paths.get("src/docs"))
				.target(Paths.get("target/docs"))
				.layout(Paths.get("assets/default.jade"))

				.build()
				.watch()
				;

	}

}
