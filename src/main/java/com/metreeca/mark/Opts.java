/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.mark;

import org.apache.maven.plugin.logging.Log;

import java.nio.file.Path;
import java.util.Map;

/**
 * Site generation options.
 */
public interface Opts {

	/**
	 * @return the path of source site folder
	 */
	public Path source();

	/**
	 * @return the path of target site folder
	 */
	public Path target();

	/**
	 * @return the path of the overlay assets folder to be merged with the {@linkplain #source() source} site folder
	 */
	public Path assets();

	/**
	 * @return the path of the default site layout, relative to the {@linkplain #source() source} site folder
	 */
	public Path layout();


	/**
	 * @return the shared variables
	 */
	public Map<String, Object> shared();

	/**
	 * @return the system logger
	 */
	public Log logger();

}
