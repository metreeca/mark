/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import java.nio.file.Path;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Site page.
 */
public abstract class Page {

	private final Path path;
	private final Map<String, Object> model;


	protected Page(final Path path) {
		this(path, emptyMap());
	}

	protected Page(final Path path, final Map<String, Object> model) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		this.path=path;
		this.model=model;
	}


	public Path path() {
		return path;
	}

	public Map<String, Object> model() {
		return unmodifiableMap(model);
	}


	public abstract void render(final Path target, final Map<String, Object> model);

}
