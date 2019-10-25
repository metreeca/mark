/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.processors;

import com.metreeca.mark.Processor;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Text;
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
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


public final class Page implements Processor {

	private static final String JadeExtension=".jade";
	private static final Pattern ExpressionPattern=Pattern.compile("(\\\\)?#\\{([^}]*)}");

	private final Path root;
	private final Path layout;

	private final Parser.Builder parsers;
	private final HtmlRenderer.Builder renderers;

	private final JadeConfiguration jade;


	public Page(final Path root, final Path layout, final Map<String, Object> shared) {

		this.root=root;
		this.layout=layout;
		this.jade=new JadeConfiguration();


		final MutableDataSet options=new MutableDataSet()

				.set(Parser.EXTENSIONS, singleton(YamlFrontMatterExtension.create()));

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

	@Override public String process(final Path source, final Path target) {

		final String path=target.toString();
		final String base=Optional.of(path.lastIndexOf('.')).map(dot -> path.substring(0, dot)).orElse(path);
		final String type=path.substring(base.length());

		if ( type.equals(".md") ) {

			try (
					final BufferedReader reader=Files.newBufferedReader(source, UTF_8);
					final BufferedWriter writer=Files.newBufferedWriter(Paths.get(base+".html"), UTF_8)
			) {

				final Node document=parsers.build().parseReader(reader);

				final Map<String, Object> model=model(document);
				final String content=content(document, model);

				model.put("base", target.relativize(root));
				model.put("content", content);
				model.put("headings", headings(document));

				jade.renderTemplate(jade.getTemplate(layout.toString()), singletonMap("page", model), writer);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

			return "markdown+jade";

		} else {

			return "";

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<String, Object> model(final Node document) {

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

				if ( matcher.group(1) != null ) { // escaped

					builder.append(chars.subSequence(start, end)); // expression text

				} else {

					try {

						builder.append(SubSequence.of(handler.evaluateStringExpression(
								matcher.group(2), new JadeModel(model) // expression value
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

		public List<Page.Section> getSections();

	}

}
