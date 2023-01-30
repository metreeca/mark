/*
 * Copyright © 2019-2023 Metreeca srl
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

import com.metreeca.mark.Mark;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ext.admonition.AdmonitionExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiShortcutType;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.*;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


/**
 * Markdown to HTML conversion.
 */
public final class Markdown {

	public static final DataKey<Boolean> SmartLinks=new DataKey<>("markdown-smart-links", false);
	public static final DataKey<Boolean> ExternalLinks=new DataKey<>("markdown-external-links", false);


	private static final Pattern URLPattern=Pattern.compile("(?<=^|/)(index|([^/#]*))\\.md(#[^/#]*)?$");
	private static final Pattern TrimPattern=Pattern.compile("^\\s*\"\\s*|\\s*\"\\s*$");


	static String plain(final CharSequence url) {
		return url == null ? null : URLPattern.matcher(url).replaceAll("$1.html$3");
	}

	static String smart(final CharSequence url) {
		return url == null ? null : Optional.of(URLPattern.matcher(url).replaceAll("$2$3"))
				.filter(smart -> !smart.isEmpty())
				.orElse(".");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Parser.Builder parsers;
	private final HtmlRenderer.Builder renderers;


	public Markdown(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

		final MutableDataSet options=new MutableDataSet()

				.set(Parser.EXTENSIONS, asList(
						YamlFrontMatterExtension.create(),
						TocExtension.create(),
						TablesExtension.create(),
						DefinitionExtension.create(),
						AdmonitionExtension.create(),
						AutolinkExtension.create(),
						LinkRewriterExtension.create(),
						EmojiExtension.create()
				))

				.set(HtmlRenderer.RENDER_HEADER_ID, true)
				.set(HtmlRenderer.GENERATE_HEADER_ID, true)
				.set(HtmlRenderer.HEADER_ID_GENERATOR_NO_DUPED_DASHES, true)
				.set(HtmlRenderer.HEADER_ID_GENERATOR_RESOLVE_DUPES, true)

				.set(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.GITHUB)

				.set(SmartLinks, mark.option(SmartLinks.getName(), Boolean::parseBoolean))
				.set(ExternalLinks, mark.option(ExternalLinks.getName(), Boolean::parseBoolean));

		this.parsers=Parser.builder(options);
		this.renderers=HtmlRenderer.builder(options);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Map<String, Object> read(final Path source) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		try ( final BufferedReader reader=Files.newBufferedReader(source, UTF_8) ) {

			final Node document=parsers.build().parseReader(reader);

			final Map<String, Object> model=metadata(document);

			model.put("headings", headings(document));
			model.put("body", body(document));

			return model;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<String, Object> metadata(final Node document) {

		final AbstractYamlFrontMatterVisitor visitor=new AbstractYamlFrontMatterVisitor();

		visitor.visit(document);

		return visitor.getData().entrySet().stream().collect(toMap(

				field -> field.getKey().trim(),

				// ;(idea) IDEA-257666 formatting a markdown file scrambles the yaml frontmatter.

				field -> field.getValue().stream()
						.map(value -> TrimPattern.matcher(value).replaceAll(""))
						.collect(toList())
		));
	}

	private List<Map<String, Object>> headings(final Node document) {

		final List<Heading> headings=new ArrayList<>();

		new NodeVisitor(new VisitHandler<>(Heading.class, headings::add)).visit(document);

		return headings.stream()
				.map(HeadingMap::new)
				.collect(toList());
	}

	private String body(final Node document) {
		return renderers.build().render(document);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class HeadingMap extends AbstractMap<String, Object> {

		private final Heading heading;


		private HeadingMap(final Heading heading) { this.heading=heading; }


		@NotNull @Override public Set<Entry<String, Object>> entrySet() {
			return Set.of(
					entry("level", heading.getLevel()),
					entry("id", heading.getAnchorRefId()),
					entry("text", heading.getAnchorRefText())
			);
		}

	}

}
