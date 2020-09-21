/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import com.metreeca.mark.tasks.*;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;


public final class MarkTest {

	public static void main(final String... args) {
		new Mark(new TestOpts())
				.exec(new Build())
				.exec(new Serve())
				.exec(new Watch());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs private static <K, V> Map<K, V> map(final Map.Entry<K, V>... entries) {
		return Stream.of(entries).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private static <K, V> Map.Entry<K, V> entry(final K key, final V value) {
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
	}


	private static final class TestOpts implements Opts {

		@Override public Path source() { return Paths.get("src/test/samples"); }

		@Override public Path target() { return Paths.get("target/docs"); }

		@Override public Path assets() { return Paths.get("src/main/themes/docs"); }

		@Override public Path layout() { return Paths.get("layouts/default.pug"); }


		@Override public Map<String, Object> shared() {
			return map(
					entry("project", map(

							entry("groupId", "com.metreeca"),
							entry("artifactId", "metreeca-work"),
							entry("version", "1.2.3-SNAPSHOT"),

							entry("name", "Metreeca Static Site Generator"),
							entry("description", "A minimalist static site generator."),
							entry("url", "https://github.com/metreeca/mark"),

							entry("organization", map(
									entry("name", "Metreeca"),
									entry("url", "https://www.metreeca.com/")
							))

					))
			);
		}

		@Override public Log logger() { return new SystemStreamLog(); }

	}

}