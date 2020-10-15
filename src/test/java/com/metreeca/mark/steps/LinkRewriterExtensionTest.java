/*
 * Copyright © 2019-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.mark.steps;

import org.junit.jupiter.api.Test;

import static com.metreeca.mark.steps.LinkRewriterExtension.plain;
import static com.metreeca.mark.steps.LinkRewriterExtension.smart;
import static org.assertj.core.api.Assertions.assertThat;

final class LinkRewriterExtensionTest {

	@Test void testPlainRewriting() {

		assertThat(plain("link.md")).isEqualTo("link.html");
		assertThat(plain("path/link.md")).isEqualTo("path/link.html");
		assertThat(plain("path/link.md#hash")).isEqualTo("path/link.html#hash");

		assertThat(plain("index.md")).isEqualTo("index.html");
		assertThat(plain("path/index.md")).isEqualTo("path/index.html");
		assertThat(plain("path/index.md#hash")).isEqualTo("path/index.html#hash");

		assertThat(plain("reindex.md")).isEqualTo("reindex.html");

	}

	@Test void testSmartRewriting() {

		assertThat(smart("link.md")).isEqualTo("link");
		assertThat(smart("path/link.md")).isEqualTo("path/link");
		assertThat(smart("path/link.md#hash")).isEqualTo("path/link#hash");

		assertThat(smart("index.md")).isEqualTo(".");
		assertThat(smart("path/index.md")).isEqualTo("path/");
		assertThat(smart("path/index.md#hash")).isEqualTo("path/#hash");

		assertThat(smart("reindex.md")).isEqualTo("reindex");

	}

}