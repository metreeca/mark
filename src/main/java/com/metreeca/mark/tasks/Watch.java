/*
 * Copyright © 2019-2022 Metreeca srl
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

package com.metreeca.mark.tasks;

import com.metreeca.mark.*;

import java.util.stream.Stream;

/**
 * Site watching task.
 *
 * <p>Generates a processed version of the {@linkplain Opts#source() source} site folder in the
 * {@linkplain Opts#target() target} site folder, watching the {@linkplain Opts#source() source} site folder for
 * further changes to by synchronized.</p>
 */
public final class Watch implements Task {

	@Override public void exec(final Mark mark) {

		mark.watch((kind, path) -> {

			if ( mark.isLayout(path) ) { mark.exec(new Build()); } else { mark.process(Stream.of(path)); }

		});

		try {

			Thread.sleep(Long.MAX_VALUE);

		} catch ( final InterruptedException e ) {

			mark.logger().error("interrupted…");

		}

	}

}
