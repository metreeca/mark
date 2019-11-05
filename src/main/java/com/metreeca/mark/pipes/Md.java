/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.pipes;

import com.metreeca.mark.Mark;
import com.metreeca.mark.Pipe;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Link;
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
import com.vladsch.flexmark.util.sequence.CharSubSequence;
import com.vladsch.flexmark.util.sequence.SubSequence;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.exceptions.ExpressionException;
import de.neuland.jade4j.expression.ExpressionHandler;
import de.neuland.jade4j.model.JadeModel;
import de.neuland.jade4j.template.TemplateLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.mark.Mark.basename;
import static com.metreeca.mark.Mark.extension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


public final class Md implements Pipe {

	private static final String JadeExtension=".jade";

	private static final Pattern ExpressionPattern=Pattern.compile("\\\\?\\$\\{([.\\w]+)}");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Mark mark;

	private final Parser.Builder parsers;
	private final HtmlRenderer.Builder renderers;

	private final JadeConfiguration jade;


	public Md(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

		this.mark=mark;

		this.jade=new JadeConfiguration();


		final MutableDataSet options=new MutableDataSet()

				.set(Parser.EXTENSIONS, asList(
						YamlFrontMatterExtension.create(),
						TocExtension.create(),
						TablesExtension.create(),
						DefinitionExtension.create(),
						AutolinkExtension.create()
				))

				.set(HtmlRenderer.RENDER_HEADER_ID, true)
				.set(HtmlRenderer.GENERATE_HEADER_ID, true)
				.set(HtmlRenderer.HEADER_ID_GENERATOR_NO_DUPED_DASHES, true)
				.set(HtmlRenderer.HEADER_ID_GENERATOR_RESOLVE_DUPES, true);

		this.parsers=Parser.builder(options);
		this.renderers=HtmlRenderer.builder(options);


		jade.setPrettyPrint(false);
		jade.setSharedVariables(mark.shared());

		jade.setTemplateLoader(new TemplateLoader() {

			@Override public long getLastModified(final String name) throws IOException {
				return Files.getLastModifiedTime(mark.layout(name)).toMillis();
			}

			@Override public Reader getReader(final String name) throws IOException {
				return Files.newBufferedReader(mark.layout(name), UTF_8);
			}

			@Override public String getExtension() {
				return JadeExtension.substring(1);
			}

		});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean process(final Path source, final Path target) {
		if ( extension(source).endsWith(".md") ) {

			final Path effective=target.resolveSibling(basename(target)+".html");

			try (
					final BufferedReader reader=Files.newBufferedReader(source, UTF_8);
					final BufferedWriter writer=Files.newBufferedWriter(effective, UTF_8)
			) {

				final Node document=parsers.build().parseReader(reader);

				new NodeVisitor(new VisitHandler<>(Link.class, link -> { // !!! as post-processing extension

					final BasedSequence url=link.getUrl();

					if ( url.endsWith(".md") ) {
						link.setUrl(url.removeProperSuffix(".md").append(".html"));
					}

				})).visit(document);

				final Map<String, Object> model=metadata(document);

				model.computeIfAbsent("date", key -> ISO_LOCAL_DATE.format(LocalDate.now()));

				model.put("base", mark.base(effective).toString());
				model.put("path", mark.path(effective).toString());
				model.put("headings", headings(document));
				model.put("content", content(document, singletonMap("page", model)));

				jade.renderTemplate(
						jade.getTemplate(model.getOrDefault("layout", "").toString()),
						singletonMap("page", model),
						writer
				);

				return true;

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		} else {

			return false;

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

	private String content(final Node document, final Map<String, Object> model) {

		//new NodeVisitor(new VisitHandler<?>[]{}) { // ;( unable to match abstract node classes with VisitHandler
		//
		//	@Override public void visit(final Node node) {
		//
		//		super.visit(node);
		//
		//		if ( node instanceof ContentNode ) {
		//
		//			((ContentNode)node).setContentLines(((ContentNode)node).getContentLines()
		//					.stream()
		//					.map(line -> evaluate(line, model))
		//					.collect(toList())
		//			);
		//
		//		} else {
		//
		//			node.setChars(evaluate(node.getChars(), model));
		//
		//		}
		//	}
		//
		//}.visit(document);

		return renderers.build().render(document);
	}


	private BasedSequence evaluate(final CharSequence chars, final Map<String, Object> model) {

		final Matcher matcher=ExpressionPattern.matcher(chars);
		final ExpressionHandler handler=jade.getExpressionHandler();

		final StringBuilder builder=new StringBuilder(chars.length());

		int last=0;

		while ( matcher.find() ) {

			final int start=matcher.start();
			final int end=matcher.end();

			builder.append(chars.subSequence(last, start)); // leading text

			if ( matcher.group().charAt(0) == '\\' ) { // escaped

				builder.append(chars.subSequence(start+1, end)); // expression text

			} else {

				try {

					final JadeModel bindings=new JadeModel(jade.getSharedVariables());

					bindings.putAll(model);

					builder.append(SubSequence.of(handler.evaluateStringExpression(
							matcher.group(1), bindings // expression value
					)));

				} catch ( ExpressionException e ) {
					throw new RuntimeException(e);
				}

			}

			last=end;
		}

		builder.append(chars.subSequence(last, chars.length())); // trailing text

		return CharSubSequence.of(builder);
	}

}
