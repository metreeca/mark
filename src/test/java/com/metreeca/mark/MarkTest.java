/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

 final class MarkTest {

	@Test void build() {

		new Mark()

				.source(Paths.get("src/docs"))
				.target(Paths.get("target/docs"))
				.layout(Paths.get("assets/default.jade"))

				.build();

	}

	 @Test void watch() {

		 new Mark()

				 .source(Paths.get("src/docs"))
				 .target(Paths.get("target/docs"))
				 .layout(Paths.get("assets/default.jade"))

				 .build()
				 .watch();

	 }

}
