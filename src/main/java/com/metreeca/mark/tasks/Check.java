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

import org.apache.maven.plugin.logging.Log;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.*;

import static com.metreeca.rest.handlers.Publisher.variants;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Site link checking task.
 *
 * <p>Scans HTML files in the {@linkplain Opts#target() target} site folder and reports dangling links.</p>
 */
public final class Check implements Task {

	private static final Pattern URLPattern=Pattern.compile("^\\w+:.*$");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void exec(final Mark mark) {

		final Path target=mark.target();
		final Log logger=mark.logger();

		try ( final Stream<Path> walk=Files.walk(target) ) {

			final long start=currentTimeMillis();

			final List<Map.Entry<String, String>> links=walk
					.filter(Files::isRegularFile)
					.flatMap(path -> scan(target, path))
					.collect(toList());

			final Set<String> internal=links.stream() // verified internal link targets
					.map(Map.Entry::getKey)
					.collect(toSet());

			final Set<String> external=links.stream() // verified external link targets
					.map(Map.Entry::getValue)
					.filter(URLPattern.asPredicate())

					// !!! external links

					//.distinct()
					//.filter(url -> {
					//	try {
					//
					//		logger.info(format("checking %s", url));
					//
					//		return validate(url);
					//
					//	} catch ( final IOException e ) {
					//
					//		logger.warn(e.toString());
					//
					//		return false;
					//
					//	}
					//})

					.collect(toSet());

			final long broken=links.stream()

					.filter(link -> variants(link.getValue()).noneMatch(internal::contains))
					.filter(link -> Stream.of(link.getValue()).noneMatch(external::contains))

					.peek(link -> logger.warn(format("%s ~› %s", link.getKey(), link.getValue())))

					.count();

			final long stop=currentTimeMillis();

			if ( broken > 0 ) {
				logger.warn(format("%d broken links", broken));
			} else {
				logger.info("no broken links");
			}

			if ( !links.isEmpty() ) {
				logger.info(format("processed %,d files in %,.3f s", links.size(), (stop-start)/1000.0f));
			}

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Stream<Map.Entry<String, String>> scan(final Path base, final Path path) {

		final String self=base.relativize(path).toString();

		if ( path.toString().endsWith(".html") ) {

			final Document document=document(path);

			return Stream.of(

					Stream.of(new SimpleImmutableEntry<>(self, self)), // document self flink

					nodes(document, "//@id|//html:a/@name") // anchors self links
							.map(Node::getTextContent)
							.map(anchor -> self+"#"+anchor)
							.map(anchor -> new SimpleImmutableEntry<>(anchor, anchor)),

					nodes(document, "//@href|//@src") // actual links
							.map(Node::getTextContent)
							.map(link -> URLPattern.matcher(link).matches() ? link
									: link.startsWith("//") ? "http:"+link
									: link.startsWith("#") ? self+link
									: base.relativize(path.resolveSibling(clean(link)).normalize()).toString()
							)
							.map(link -> new SimpleImmutableEntry<>(self, link))

			).flatMap(stream -> stream);

		} else {

			return Stream.of(new SimpleImmutableEntry<>(self, self)); // document self flink

		}

	}


	private String clean(final String link) { // fix glitches in local links
		return index(query(link));
	}

	private String index(final String link) { // add missing index files

		return link.endsWith("/") ? link+"index.html"
				: link.endsWith(".") ? link+"/index.html"
				: link;

	}

	private String query(final String link) { // remove query components (e.g. javadocs)

		final int question=link.indexOf('?');

		return question >= 0 ? link.substring(0, question) : link;
	}


	private Document document(final Path path) {
		try ( final InputStream stream=Files.newInputStream(path) ) {

			final String uri=path.toUri().toString();

			final InputSource input=new InputSource();

			input.setSystemId(uri);
			input.setByteStream(stream);

			final Document document=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			document.setDocumentURI(uri);

			TransformerFactory.newInstance().newTransformer().transform(

					new SAXSource(new Parser(), input),
					new DOMResult(document)

			);

			return document;

		} catch ( final ParserConfigurationException e ) {

			throw new RuntimeException("unable to create document builder", e);

		} catch ( final TransformerConfigurationException e ) {

			throw new RuntimeException("unable to create transformer", e);

		} catch ( final TransformerException e ) {

			throw new RuntimeException("unable to parse document", e);

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

	private Stream<Node> nodes(final Document document, final String query) {
		try {

			final XPath xpath=XPathFactory

					.newInstance()
					.newXPath();

			xpath.setNamespaceContext(new NamespaceContext() {

				@Override public String getNamespaceURI(final String prefix) {
					return prefix.equals("html") ? "http://www.w3.org/1999/xhtml" : null;
				}

				@Override public String getPrefix(final String namespaceURI) {
					throw new UnsupportedOperationException("prefix lookup");
				}

				@Override public Iterator<String> getPrefixes(final String namespaceURI) {
					throw new UnsupportedOperationException("prefixes lookup");
				}

			});

			return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new NodeIterator((NodeList)xpath

					.compile(query)
					.evaluate(document, XPathConstants.NODESET)

			), Spliterator.ORDERED), false);

		} catch ( final XPathExpressionException e ) {
			throw new RuntimeException("unable to evaluate xpath expression", e);
		}
	}


	private boolean validate(final String url) throws IOException {
		if ( url.startsWith("javascript:") ) {

			return true;

		} else if ( url.startsWith("http") ) {

			final HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();

			connection.setRequestMethod("HEAD");
			connection.setRequestProperty("User-Agent", ""
					+"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_6)"
					+"AppleWebKit/537.36 (KHTML, like Gecko)"
					+"Chrome/85.0.4183.102 "
					+"Safari/537.36"
			);

			connection.setInstanceFollowRedirects(true);
			connection.setConnectTimeout(2500);
			connection.setReadTimeout(2500);

			connection.connect();

			return connection.getResponseCode()/100 == 2;

		} else {

			new URL(url); // only well-formedness tests

			return true;

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class NodeIterator implements Iterator<Node> {

		private final NodeList nodes;
		private int next;


		private NodeIterator(final NodeList nodes) { this.nodes=nodes; }


		@Override public boolean hasNext() {
			return next < nodes.getLength();
		}

		@Override public Node next() throws NoSuchElementException {

			if ( !hasNext() ) {
				throw new NoSuchElementException("no more iterator elements");
			}

			return nodes.item(next++);

		}

	}

}
