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

package com.metreeca.mark;

/**
 * TOC item.
 */
public final class Item {

	private final int level;
	private final String anchorRefId;
	private final String label;


	public Item(final int level, final String anchor, final String label) {

		if ( level < 0 || level > 6 ) {
			throw new IllegalArgumentException("illegal level");
		}

		if ( anchor == null ) {
			throw new NullPointerException("null anchor");
		}

		this.level=level;
		this.anchorRefId=anchor;
		this.label=label;
	}


	public int level() {
		return level;
	}

	public String anchor() {
		return anchorRefId;
	}

	public String label() {
		return label;
	}

}
