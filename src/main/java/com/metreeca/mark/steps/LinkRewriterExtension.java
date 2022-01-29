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

package com.metreeca.mark.steps;

import com.vladsch.flexmark.html.*;
import com.vladsch.flexmark.html.renderer.LinkResolverBasicContext;
import com.vladsch.flexmark.html.renderer.ResolvedLink;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataHolder;

import java.util.Set;

import static com.metreeca.mark.steps.Markdown.plain;
import static com.metreeca.mark.steps.Markdown.smart;

/**
 * Flexmark link rewriting extension.
 */
final class LinkRewriterExtension implements HtmlRenderer.HtmlRendererExtension {

	static LinkRewriterExtension create() {
		return new LinkRewriterExtension();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void rendererOptions(final MutableDataHolder options) {}

	@Override public void extend(final HtmlRenderer.Builder htmlRendererBuilder, final String rendererType) {
		htmlRendererBuilder.linkResolverFactory(new LinkRewriterFactory());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class LinkRewriterFactory implements LinkResolverFactory {

		@Override public Set<Class<?>> getAfterDependents() { return null; }

		@Override public Set<Class<?>> getBeforeDependents() { return null; }

		@Override public boolean affectsGlobalScope() { return false; }

		@Override public LinkResolver apply(final LinkResolverBasicContext context) {
			return new LinkRewriter(context);
		}

	}

	private static final class LinkRewriter implements LinkResolver {

		private final boolean smart;
		private final boolean external;


		private LinkRewriter(final LinkResolverBasicContext context) {
			smart=Markdown.SmartLinks.get(context.getOptions());
			external=Markdown.ExternalLinks.get(context.getOptions());
		}


		@Override public ResolvedLink resolveLink(
				final Node node, final LinkResolverBasicContext context, final ResolvedLink link
		) {

			final String url=link.getUrl();
			final String target=link.getTarget();

			return link
					.withUrl(smart ? smart(url) : plain(url))
					.withTarget(external && url.startsWith("http") && target == null ? "_blank" : target);

		}

	}

}
