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

import { File } from "@metreeca/mark/nests/file";
import MarkDown from "@metreeca/mark/tiles/down";
import { Ban, HeartCrack, Lock, Menu, RefreshCw, Stethoscope, X } from "lucide-react";
import React, { createElement, useState } from "react";
import "./page.css";

// !!! version
// !!! github link

export default function MarkPage({

	children: {

		name,
		logo,
		home,

		version,
		description,

		creator,
		copyright,
		license,
		licenseURI,

		snippets,
		sections,

		path,
		code,
		text

	}

}: {

	children: File

}) {

	const [tray, setTray]=useState(false);

	return createElement("mark-page", {

		class: tray ? "tray" : undefined

	}, <>

		<nav onClick={e => {

			if ( e.target instanceof HTMLElement && (e.target as HTMLElement).closest("a") ) {
				setTray(false);
			}

		}}>

			<header>

				<a href={home || "/"}>{name}</a>

				<button onClick={e => {

					e.currentTarget.closest("mark-page")?.scrollTo({ top: 0 });

					setTray(!tray);

				}}>{tray ? <X/> : <Menu/>}</button>

			</header>

			<section onClick={e => { // force scroll to header

				if ( e.target instanceof HTMLAnchorElement ) {

					const href=e.target.getAttribute("href"); // ;( not resolved against location

					if ( href?.startsWith("#") ) {
						document.getElementById(href?.substring(1))?.scrollIntoView();
					}

				}

			}}>

				{version && <span>v{version.replace(/^v/, "")}</span>}

				{Object.entries(sections ?? {}).map(([label, link]) =>
					<a key={link} href={link}>{label}</a>
				)}

				<hr/>

				<MarkDown meta={"toc"}>{text}</MarkDown>

			</section>

			<footer>

				{logo && (creator?.match(/^\w+:/)
						? <a className={"logo"} href={creator} style={{ backgroundImage: `url("${logo}")` }}/>
						: <span className={"logo"} style={{ backgroundImage: `url("${logo}")` }}/>
				)}

			</footer>

		</nav>

		<main>

			<header>{

				{

					0: "Loading…",

					401: `${code} Restricted Document`,
					403: `${code} Forbidden Document`,
					404: `${code} Document Not Found`,

					200: <MarkDown meta={({ title }) =>

						<a href={path} onClick={e => {

							e.currentTarget.closest("main")?.scrollTo({ top: 0 });

						}}>{title || description}</a>

					}>{text}</MarkDown>

				}[code ?? 0] ?? `${code} Download Error`

			}</header>

			<section>{

				{

					0: <RefreshCw className={"spin"}/>,

					401: <Lock/>,
					403: <Ban/>,
					404: <HeartCrack/>,

					200: <MarkDown meta={snippets}>{text}</MarkDown>

				}[code ?? 0] ?? <Stethoscope/>

			}</section>

			<footer>

				{copyright}

				{copyright && license && ". "}

				{license && <>This work is licensed under the {licenseURI
					? <a href={licenseURI}>{license}</a>
					: license
				}</>}

			</footer>

		</main>

	</>);
}
