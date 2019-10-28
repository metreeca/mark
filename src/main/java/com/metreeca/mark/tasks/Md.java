/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.tasks;

import com.metreeca.mark.Mark;
import com.metreeca.mark.Task;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.SubSequence;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.exceptions.ExpressionException;
import de.neuland.jade4j.expression.ExpressionHandler;
import de.neuland.jade4j.model.JadeModel;
import de.neuland.jade4j.template.TemplateLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.mark.Mark.target;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


public final class Md implements Task {

	private static final String JadeExtension=".jade";
	private static final Pattern ExpressionPattern=Pattern.compile("\\\\?\\$\\{([.\\w]+)}");


	private final Path root;
	private final Path layout;

	private final Parser.Builder parsers;
	private final HtmlRenderer.Builder renderers;

	private final JadeConfiguration jade;


	public Md(final Path root, final Path layout, final Map<String, Object> shared) {

		this.root=root;
		this.layout=layout;
		this.jade=new JadeConfiguration();


		final MutableDataSet options=new MutableDataSet()

				.set(Parser.EXTENSIONS, asList(
						YamlFrontMatterExtension.create(),
						TablesExtension.create()
				))

				.set(HtmlRenderer.RENDER_HEADER_ID, true)
				.set(HtmlRenderer.GENERATE_HEADER_ID, true)
				.set(HtmlRenderer.HEADER_ID_GENERATOR_NO_DUPED_DASHES, true)
				.set(HtmlRenderer.HEADER_ID_GENERATOR_RESOLVE_DUPES, true);

		this.parsers=Parser.builder(options);
		this.renderers=HtmlRenderer.builder(options);


		jade.setPrettyPrint(false);
		jade.setSharedVariables(shared);

		jade.setTemplateLoader(new TemplateLoader() {

			private final Path assets=layout.getParent();


			@Override public long getLastModified(final String name) throws IOException {
				return Files.getLastModifiedTime(resolve(name)).toMillis();
			}

			@Override public Reader getReader(final String name) throws IOException {
				return Files.newBufferedReader(resolve(name), UTF_8);
			}

			@Override public String getExtension() {
				return JadeExtension.substring(1);
			}


			private Path resolve(final String name) {
				return verify(assets.resolve(name.endsWith(JadeExtension) ? name : name+JadeExtension));
			}

			private Path verify(final Path path) {

				if ( !path.toAbsolutePath().startsWith(assets) ) {
					throw new IllegalArgumentException("layout outside source folder {"+path+"}");
				}

				return path;
			}

		});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean process(final Path source, final Path target) {
		if ( source.toString().endsWith(".md") ) {

			try (
					final BufferedReader reader=Files.newBufferedReader(source, UTF_8);
					final BufferedWriter writer=Files.newBufferedWriter(target(target, ".html"), UTF_8)
			) {

				final Node document=parsers.build().parseReader(reader);

				final Map<String, Object> model=metadata(document);

				model.put("base", Mark.base(target, root));
				model.put("content", content(document, model));
				model.put("headings", headings(document));

				jade.renderTemplate(jade.getTemplate(layout.toString()), singletonMap("page", model), writer);

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

	private String content(final Node document, final Map<String, Object> model) {

		new NodeVisitor(new VisitHandler<>(Text.class, text -> {

			final BasedSequence chars=text.getChars();

			final Matcher matcher=ExpressionPattern.matcher(chars);
			final ExpressionHandler handler=jade.getExpressionHandler();

			final StringBuilder builder=new StringBuilder(chars.length());

			int last=0;

			while ( matcher.find() ) {

				final int start=matcher.start();
				final int end=matcher.end();

				builder.append(chars.subSequence(last, start)); // leading text

				if ( matcher.group() .charAt(0) == '\\' ) { // escaped

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

			text.setChars(SubSequence.of(builder));

		})).visit(document);

		return renderers.build().render(document);
	}

	private Object headings(final Node document) {

		final List<Section> stack=new ArrayList<>();

		new NodeVisitor(new VisitHandler<>(Heading.class, heading -> {

			if ( heading.getLevel() == 1 ) {
				//headings.add(heading.getText().toString());
			}

		})).visit(document);

		return stack;
	}


	public static interface Section {

		public String getLabel();

		public List<Md.Section> getSections();

	}

}
