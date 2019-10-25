/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Mark.
 *
 * Metreeca/Mark is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Mark is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Mark.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.mark;

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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


public final class Mark {

	private static final Logger logger=Logger.getLogger(Mark.class.getName());

	private static final String JadeExtension=".jade";
	private static final Pattern ExpressionPattern=Pattern.compile("(\\\\)?#\\{([^}]*)}");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Path source;
	private final Path target;
	private final Path layout;


	private final Parser.Builder parsers;
	private final HtmlRenderer.Builder renderers;

	private final JadeConfiguration jade;


	public Mark(final Path source, final Path target, final Path layout, final Map<String, Object> defaults) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		if ( !Files.exists(source) ) {
			throw new IllegalArgumentException("missing source folder {"+source+"}");
		}

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		if ( layout == null ) {
			throw new NullPointerException("null layout");
		}

		if ( defaults == null ) {
			throw new NullPointerException("null defaults");
		}

		this.source=source.toAbsolutePath().normalize();
		this.target=target.toAbsolutePath().normalize();

		if ( this.target.startsWith(this.source) || this.source.startsWith(this.target) ) {
			throw new IllegalArgumentException("overlapping source/target folders {"+source+" <-> "+target+"}");
		}

		this.layout=source.resolve(layout).toAbsolutePath();

		final MutableDataSet options=new MutableDataSet()

				.set(Parser.EXTENSIONS, singleton(YamlFrontMatterExtension.create()));

		this.parsers=Parser.builder(options);
		this.renderers=HtmlRenderer.builder(options);

		this.jade=new JadeConfiguration();


		jade.setPrettyPrint(true);
		jade.setSharedVariables(defaults);

		jade.setTemplateLoader(new TemplateLoader() {

			private final Path assets=Mark.this.layout.getParent();


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

				if ( !path.toAbsolutePath().startsWith(assets)) {
					throw new IllegalArgumentException("layout outside source folder {"+path+"}");
				}

				return path;
			}

		});

	}


	public void process() {

		if ( Files.exists(target) ) {
			try (final Stream<Path> walk=Files.walk(target)) { // clean target folder

				walk.sorted(reverseOrder()).filter(isEqual(target).negate()).forEachOrdered(path -> {

					try {

						Files.delete(path);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}

				});

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}

		if ( Files.exists(source) ) {

			try (final Stream<Path> walk=Files.walk(source)) { // process source folder

				walk.filter(Files::isRegularFile).forEach(path -> {

					try {

						process(path);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e); // !!! report
					}

				});

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void process(final Path source) throws IOException {

		final Path target=this.target.resolve(this.source.relativize(source));

		Files.createDirectories(target.getParent());

		final String path=target.toString();
		final String base=Optional.of(path.lastIndexOf('.')).map(dot -> path.substring(0, dot)).orElse(path);
		final String type=path.substring(base.length());

		if ( type.equals(".md") ) {

			logger.info(() -> String.format("processing <%s>", source));

			markdown(source, Paths.get(base+".html"));

		} else if ( !(source.startsWith(layout.getParent()) // ignore templates
				&& source.toString().endsWith("."+jade.getTemplateLoader().getExtension())) ) {

			logger.info(() -> String.format("copying <%s>", source));

			Files.copy(source, target);

		}

	}

	private void markdown(final Path source, final Path target) throws IOException {
		try (
				final BufferedReader reader=Files.newBufferedReader(source, UTF_8);
				final BufferedWriter writer=Files.newBufferedWriter(target, UTF_8)
		) {

			final Node document=parsers.build().parseReader(reader);

			final Map<String, Object> model=model(document);
			final String content=content(document, model);

			model.put("base", target.relativize(this.target));
			model.put("content", content);
			model.put("headings", headings(document));

			jade.renderTemplate(jade.getTemplate(layout.toString()), singletonMap("page", model), writer);

		}
	}


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

		public List<Section> getSections();

	}

}
