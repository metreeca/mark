/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.steps;

import com.metreeca.mark.Mark;

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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Collections.singletonMap;


public final class Jade {

	private static final Pattern ExpressionPattern=Pattern.compile("\\\\?\\$\\{([.\\w]+)}");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Mark mark;

	private final JadeConfiguration jade;


	public Jade(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

		this.mark=mark;
		this.jade=new JadeConfiguration();

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
				return "jade";
			}

		});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Path write(final Path target, final Map<String, Object> model) {

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		final String layout=model.getOrDefault("layout", mark.layout()).toString();

		try ( final BufferedWriter writer=Files.newBufferedWriter(target, UTF_8) ) {

			jade.renderTemplate(jade.getTemplate(layout), page(model, target), writer);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}

		return target;
	}


	//// Variable Replacement //////////////////////////////////////////////////////////////////////////////////////////

	private Map<String, Object> page(final Map<String, Object> model, final Path target) {

		model.put("base", mark.base(target).toString());
		model.put("path", mark.path(target).toString());

		model.computeIfAbsent("date", key -> ISO_LOCAL_DATE.format(LocalDate.now()));
		model.computeIfPresent("content", (key, content) -> evaluate(content.toString(), model));

		return singletonMap("page", model);
	}

	private String evaluate(final CharSequence chars, final Map<String, Object> model) {

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

				} catch ( final ExpressionException e ) {
					throw new RuntimeException(e);
				}

			}

			last=end;
		}

		builder.append(chars.subSequence(last, chars.length())); // trailing text

		return builder.toString();
	}

}
