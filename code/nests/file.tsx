/*
 * Copyright © 2020-2024 Metreeca srl
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

import { ReactNode, useEffect, useState } from "react";


const Meta: Meta={

	name: title(),
	logo: link("icon"),
	home: link("home")?.replace(/\/?$/, "/"),

	description: meta("description"),
	version: meta("version"),

	creator: meta("creator"),
	publisher: meta("publisher"),

	copyright: meta("copyright"),
	license: meta("license"),
	licenseURI: meta("license:uri"),

	sections: sections()

};


function title() {
	return document.title || undefined;
}

function link(rel: string) {
	return document.querySelector<HTMLLinkElement>(`link[rel='${rel}']`)?.href.replace(location.origin, "") || undefined;
}

function meta(name: string) {
	return document.querySelector<HTMLMetaElement>(`meta[name='${name}']`)?.content || undefined;
}


function sections() {

	const home=link("home")?.replace(/\/?$/, "/") ?? "/";
	const text=document.querySelector<HTMLScriptElement>(`script[type='application/json']`)?.text;

	const json=text ? JSON.parse(text) : undefined;

	const sections=Object.entries(json).reduce((sections, [label, link]) => {

		return typeof link === "string"
			? { ...sections, [label]: link.replace(/^\//, home) }
			: sections;

	}, {});

	return Object.keys(sections).length ? sections : undefined;
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

export interface Meta {

	readonly name?: string;
	readonly logo?: string;
	readonly home?: string;

	readonly version?: string;
	readonly description?: string;

	readonly creator?: string;
	readonly publisher?: string;

	readonly copyright?: string;
	readonly license?: string;
	readonly licenseURI?: string;

	readonly sections?: { [label: string]: string };

}

export interface Item {

	readonly path: string;
	readonly hash: string;

}

export interface Data {

	readonly code?: number;
	readonly text?: string;

}

export interface File extends Meta, Item, Data {

}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

export default function MarkFile({

	children: factory

}: {

	children: (file: File) => ReactNode

}): ReactNode {

	const [path, setPath]=useState(location.pathname);
	const [hash, setHash]=useState(location.hash.substring(1));

	const [data, setData]=useState<Data>({});


	useEffect(() => {

		const asset=path.endsWith(".md") ? path
			: path.endsWith("/") ? `${path}index.md`
				: `${path}.md`;

		const controller=new AbortController();

		fetch(asset, { signal: controller.signal })

			.then(response => response.text().then(text => {

				setData({
					code: response.status,
					text
				});

			}))

			.catch(() => {

				setData({

					code: 999

				});

			});

		return () => controller.abort();

	}, [path]);

	useEffect(() => {

		setTimeout(() => {

			if ( (location.hash=hash) ) {

				document.getElementById(hash)?.scrollIntoView();

			} else {

				document.querySelector("main")?.scrollTo({ top: 0 });

			}

		}, 0);


	}, [hash, data]);


	useEffect(() => {

		function load() {
			setPath(location.pathname);
			setHash(location.hash.substring(1));
		}

		window.addEventListener("popstate", load);

		return () => window.removeEventListener("popstate", load);

	}, []);

	useEffect(() => {

		function click(e: MouseEvent) {

			if ( !(e.altKey || e.ctrlKey || e.metaKey || e.shiftKey || e.defaultPrevented) ) { // only plain events

				const origin=e.target as Element;

				const anchor=origin.closest("a");
				const image=origin.closest("img");

				if ( anchor ) {

					e.preventDefault();

					const href=anchor.getAttribute("href");

					if ( href?.match(/^\w+:/) ) { // external navigation

						window.open(href, anchor.getAttribute("target") || "_blank");

					} else if ( href?.startsWith("#") ) { // scrolling

						setHash(href?.substring(1));

					} else { // internal navigation

						const url=new URL(anchor.href);

						try {

							history.pushState(undefined, document.title, url);

						} finally {

							setPath(url.pathname);
							setHash(url.hash.substring(1));

						}

					}

				} else if ( image ) { // drive image active visualization

					if ( image.getAttribute("active") ) {

						image.removeAttribute("active");

					} else {

						image.setAttribute("active", "true");

					}

				}

			}

		}

		window.addEventListener("click", click);

		return () => window.removeEventListener("click", click);

	}, []);


	return factory({

		...Meta,

		path,
		hash,

		...data

	});

}