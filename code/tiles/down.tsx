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

import Slugger from "github-slugger";
import { Root } from "hast";
import { headingRank } from "hast-util-heading-rank";
import { toString } from "hast-util-to-string";
import "highlight.js/styles/github.css";
import React, { ReactNode } from "react";
import ReactMarkdown, { defaultUrlTransform } from "react-markdown";
import { Nodes } from "react-markdown/lib";
import rehypeHighlight from "rehype-highlight";
import { VFile } from "rehype-highlight/lib";
import rehypeSlug from "rehype-slug";
import { remark } from "remark";
import remarkFrontmatter from "remark-frontmatter";
import remarkGemoji from "remark-gemoji";
import remarkGfm from "remark-gfm";
import { remarkAlert } from "remark-github-blockquote-alert";
import { selectAll } from "unist-util-select";

import "remark-github-blockquote-alert/alert.css";


export interface Meta {

	readonly [label: string]: undefined | string;

}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

export default function MarkDown({

	meta,

	children: text

}: {

	meta?: "toc" | ((meta: Meta) => ReactNode) | Meta

	children: undefined | string

}) {

	return !text ? null
		: meta === "toc" ? MarkTOC(text)
			: typeof meta === "function" ? MarkMeta(text, meta)
				: MarkText(text, meta);

}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

function MarkTOC(text: string) {
	return text && <ReactMarkdown

        remarkPlugins={[remarkFrontmatter]}

        rehypePlugins={[function () {
			return (root: Root) => {

				const slugs=new Slugger();

				slugs.reset();

				return {

					...root, children: (root.children).filter((node) => headingRank(node)).map(node => ({
						...node, children: [{
							...node,
							type: "element",
							tagName: "a",
							properties: { href: `#${slugs.slug(toString(node))}` }
						}]
					}))

				};

			};
		}]}

    >{

		text

	}</ReactMarkdown>;
}

function MarkMeta(text: string, meta: string | ((meta: Meta) => ReactNode)) {

	const file=remark()

		.use(remarkFrontmatter)
		.use(remarkMetadata)

		.processSync(text);

	const entries=file.data.meta && typeof file.data.meta === "object" ? file.data.meta as Meta : {};

	return meta instanceof Function ? meta(entries)
		: entries[meta] ? <span>{entries[meta]}</span>
			: null;
}

function MarkText(text: string, meta: undefined | Meta) {
	return <ReactMarkdown

		remarkPlugins={[
			remarkFrontmatter,
			remarkGfm,
			remarkAlert,
			remarkGemoji,
			remarkCodeFormatter
		]}

		rehypePlugins={[
			rehypeSlug,
			rehypeHighlight
		]}

		urlTransform={href => [defaultUrlTransform(href)]
			.map(value => value.endsWith("/index.md") ? value.substring(0, value.length - "/index.md".length) : value)
			.map(value => value.endsWith(".md") ? value.substring(0, value.length - ".md".length) : value)
			[0]
		}

	>{

		text.replace(/(\\)?\{\{meta\.([.:\w]+)}}/g, ($0, $1, $2) =>
			$1 ? $0.substring(1) : meta?.[$2] || ""
		)

	}</ReactMarkdown>;
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

function remarkMetadata() {
	return (tree: Nodes, file: VFile) => selectAll("yaml", tree).forEach(node => {

		if ( "value" in node && typeof node.value === "string" ) {

			const matches=[...node.value.matchAll(/(?:^|\n)\s*(\w+)\s*:\s*(.*?)\s*(?:\n|$)/g)];

			file.data.meta=matches.reduce((entries, [, $1, $2]) => ({

				...entries, [$1]: JSON.parse($2)

			}), {});

		}
	});
}

function remarkCodeFormatter() {
	return (tree: Nodes) => selectAll("code", tree).forEach(node => {

		if ( node && "value" in node && typeof node.value === "string" ) {

			node.value=node.value
				.replace(/^\s*|\s*$/g, "") // remove leading and traing space
				.replace(/^[ \t]+/mg, $0 => $0.replace(/ {4}|\t/g, "  ")); // compact indentation

		}

	});
}
