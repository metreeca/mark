/*
 * Copyright © 2019-2022 Metreeca srl
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

package com.metreeca.mark;

import com.metreeca.mark.steps.Markdown;
import com.metreeca.mark.tasks.*;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;

import static java.util.Map.entry;


public final class Work {

	public static void main(final String... args) {
		new Mark(new TestOpts())
				.exec(new Build())
				.exec(new Serve())
				.exec(new Watch())
		;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestOpts implements Opts {

		@Override public Path source() { return Paths.get("docs"); }

		@Override public Path target() { return Paths.get(""); }

		@Override public Path layout() { return Paths.get(""); }


		@Override public Map<String, Object> global() {
			return Map.ofEntries(
					entry("project", Map.ofEntries(

							entry("groupId", "com.metreeca"),
							entry("artifactId", "metreeca-test"),
							entry("version", "1.2.3"),

							entry("name", "Metreeca Test Project"),
							entry("description", "A dummy test project"),
							entry("url", "https://github.com/metreeca/mark"),

							entry("organization", Map.ofEntries(
									entry("name", "Metreeca"),
									entry("url", "https://www.metreeca.com/")
							))

					))
			);
		}

		@Override public <V> V get(final String option, final Function<String, V> mapper) {
			return mapper.apply(

					option.equals(Markdown.SmartLinks.getName()) ? "true"
							: option.equals(Markdown.ExternalLinks.getName()) ? "true"
							: null

			);
		}


		@Override public Log logger() { return new SystemStreamLog(); }

	}

}