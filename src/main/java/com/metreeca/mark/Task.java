/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import java.util.Optional;


@FunctionalInterface public interface Task<V, R> {

	public Optional<R> process(final V v);


	public default <T> Task<V, T> then(final Task<R, T> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return value -> process(value).flatMap(task::process);
	}

}
