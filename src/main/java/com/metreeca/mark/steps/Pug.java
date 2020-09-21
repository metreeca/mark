/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark.steps;

import com.metreeca.mark.Mark;

import com.vladsch.flexmark.util.sequence.SubSequence;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.TemplateLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;


public final class Pug {

	private static final Pattern ExpressionPattern=Pattern.compile("\\\\?\\$\\{([.\\w]+)}");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final PugConfiguration pug;


	public Pug(final Mark mark) {

		if ( mark == null ) {
			throw new NullPointerException("null mark");
		}

		this.pug=new PugConfiguration();

		pug.setPrettyPrint(false); // minified output
		pug.setTemplateLoader(new TemplateLoader() {

			@Override public long getLastModified(final String name) throws IOException {
				return Files.getLastModifiedTime(mark.layout(name)).toMillis();
			}

			@Override public Reader getReader(final String name) throws IOException {
				return Files.newBufferedReader(mark.layout(name), UTF_8);
			}

			@Override public String getExtension() {
				return "pug";
			}

			@Override public String getBase() {
				return "";
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

		final String layout=layout(model).toString();

		try ( final BufferedWriter writer=Files.newBufferedWriter(target, UTF_8) ) {

			pug.renderTemplate(pug.getTemplate(layout), set(model), writer);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}

		return target;
	}


	//// !!! refactor //////////////////////////////////////////////////////////////////////////////////////////////////

	private Object layout(final Map<String, Object> model) {

		final Object page=model.get("page");

		if ( page instanceof Map ) {

			return ((Map<String, Object>)page).getOrDefault("layout", "");

		} else {

			return "";

		}
	}

	private Map<String, Object> set(final Map<String, Object> model) {

		final Map<String, Object> _model=new HashMap<>(model);

		_model.computeIfPresent("page", (p, page) -> {

			if ( page instanceof Map ) {

				final Map<Object, Object> _page=new HashMap<>((Map<Object, Object>)page);

				_page.computeIfPresent("body", (b, body) -> evaluate(body.toString(), model));

				return _page;

			} else {

				return page;

			}

		});

		return _model;
	}


	//// Variable Replacement //////////////////////////////////////////////////////////////////////////////////////////

	private String evaluate(final CharSequence body, final Map<String, Object> model) {

		final Matcher matcher=ExpressionPattern.matcher(body);
		final ExpressionHandler handler=pug.getExpressionHandler();

		final StringBuilder builder=new StringBuilder(body.length());

		int last=0;

		while ( matcher.find() ) {

			final int start=matcher.start();
			final int end=matcher.end();

			builder.append(body.subSequence(last, start)); // leading text

			if ( matcher.group().charAt(0) == '\\' ) { // escaped

				builder.append(body.subSequence(start+1, end)); // expression text

			} else {

				try {

					final PugModel bindings=new PugModel(pug.getSharedVariables());

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

		builder.append(body.subSequence(last, body.length())); // trailing text

		return builder.toString();
	}

}
