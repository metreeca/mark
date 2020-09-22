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

package com.metreeca.mark.steps;

import com.metreeca.mark.Mark;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ext.admonition.AdmonitionExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.*;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


public final class Markdown {

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
						AutolinkExtension.create()
				))

				.set(HtmlRenderer.RENDER_HEADER_ID, true)
				.set(HtmlRenderer.GENERATE_HEADER_ID, true)
				.set(HtmlRenderer.HEADER_ID_GENERATOR_NO_DUPED_DASHES, true)
				.set(HtmlRenderer.HEADER_ID_GENERATOR_RESOLVE_DUPES, true);

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

			// !!! as post-processing extension (https://github
			// .com/vsch/flexmark-java/blob/master/flexmark-java-samples/src/com/vladsch/flexmark/java/samples
			// /SyntheticLinkSample.java)

			new NodeVisitor(new VisitHandler<>(Link.class, link -> {

				final BasedSequence url=link.getUrl();

				if ( url.endsWith(".md") ) {
					link.setUrl(url.removeProperSuffix(".md").append(".html"));
				} else {
					link.setUrl(url.replace(".md#", ".html#"));
				}

			})).visit(document);

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

		final Map<String, List<String>> metadata=visitor.getData().entrySet().stream().collect(toMap(
				e -> e.getKey().trim(), e -> e.getValue().stream().map(String::trim).collect(toList())
		));

		final Map<String, Object> model=new HashMap<>();

		metadata.forEach((name, values) -> model.put(name, new AbstractList<String>() {

			@Override public int size() { return values.size(); }

			@Override public String get(final int index) { return values.get(index); }

			@Override public String toString() { return String.join(", ", this); }

		}));

		return model;
	}

	private List<Heading> headings(final Node document) {

		final List<Heading> headings=new ArrayList<>();

		new NodeVisitor(new VisitHandler<>(Heading.class, headings::add)).visit(document);

		return headings;
	}

	private String body(final Node document) {
		return renderers.build().render(document);
	}

}
