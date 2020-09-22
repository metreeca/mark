/*
 * Copyright © 2019-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 *  file except in compliance with the License. You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.metreeca.mark;

import org.junit.jupiter.api.Test;

import static com.metreeca.mark.Mark.variants;
import static org.assertj.core.api.Assertions.assertThat;

final class MarkTest {

	@Test void testVariants() {

		assertThat(variants("link")).containsExactly("link", "link.html");
		assertThat(variants("link.html")).containsExactly("link.html");

		assertThat(variants("path/link")).containsExactly("path/link", "path/link.html");
		assertThat(variants("path/link.html")).containsExactly("path/link.html");

		assertThat(variants("path/link#hash")).containsExactly("path/link#hash", "path/link.html#hash");
		assertThat(variants("path/link.html#hash")).containsExactly("path/link.html#hash");

		assertThat(variants(".")).containsExactly("index.html");
		assertThat(variants("path/")).containsExactly("path/index.html");
		assertThat(variants("path/#hash")).containsExactly("path/index.html#hash");

		assertThat(variants("reindex")).containsExactly("reindex", "reindex.html");

	}
}