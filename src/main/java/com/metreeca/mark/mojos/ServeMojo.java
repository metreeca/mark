/*
 * Copyright © 2019-2023 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.mark.mojos;

import com.metreeca.mark.Mark;
import com.metreeca.mark.tasks.*;

import org.apache.maven.plugins.annotations.Mojo;

/**
 * Serve site goal.
 */
@Mojo(name="serve")
public final class ServeMojo extends MarkMojo {

	@Override public void execute() {
		new Mark(this)
				.exec(new Build())
				.exec(new Serve())
				.exec(new Watch());
	}

}
