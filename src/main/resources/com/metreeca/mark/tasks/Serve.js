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

(function () {

	const styles=resources("link[rel=stylesheet]");
	const scripts=resources("script");

	function resources(selector) {
		return Array.from(document.querySelectorAll(selector))
			.map(style => style.href || style.src)
			.map(url => url && new URL(url, location.href))
			.filter(url => url && url.origin === location.origin)
			.map(url => url && url.pathname)
			.filter(url => url);
	}

	function resource(path) {
		return path === location.pathname
			|| styles.some(style => style === path)
			|| scripts.some(script => script === path);
	}


	function listen() {
		fetch("/~").then(response => response.json()).then(updates => {

			console.log(updates);

			if (updates.some(resource)) {

				console.log("reloading");

				location.reload();

			} else {

				listen();

			}

		});
	}


	window.addEventListener("load", listen);

})();
