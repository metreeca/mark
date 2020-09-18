/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import com.metreeca.mark.tasks.Build;
import com.metreeca.mark.tasks.Watch;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;


final class MarkTest {

	@SafeVarargs private static <K, V> Map<K, V> map(final Map.Entry<K, V>... entries) {
		return Stream.of(entries).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private static <K, V> Map.Entry<K, V> entry(final K key, final V value) {
		return new SimpleImmutableEntry<>(key, value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static void main(final String... args) {
		new Mark(new Opts() {

			@Override public Path source() { return Paths.get("src/test/samples"); }

			@Override public Path target() { return Paths.get("target/docs"); }

			@Override public Path assets() { return Paths.get("src/main/themes/docs"); }

			@Override public Path layout() { return Paths.get("layouts/default.jade"); }


			@Override public Map<String, Object> shared() {
				return map(
						entry("project", map(

								entry("groupId", "com.metreeca"),
								entry("artifactId", "metreeca-work"),
								entry("version", "1.2.3")

						))
				);
			}

			@Override public Log logger() { return new SystemStreamLog(); }

		}).exec(new Build()).exec(new Watch());
	}

}
