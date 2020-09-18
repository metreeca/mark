/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;


/**
 * Site generation task.
 *
 * <p>Coordinates the execution of a site generation task</p>
 */
@FunctionalInterface public interface Task {

	/**
	 * Executes this task.
	 *
	 * @param mark the site generation engine
	 */
	public void exec(final Mark mark);

}
