/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.junit.Ignore;
import org.junit.Test;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.EXIReader;
import org.openexi.sax.Transmogrifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * This test case tests nagasena library used for exi encode/decode.
 *
 * This library does not work correctly, since it is impossible to encode and then decode DOM xml.
 * Encoding DOM using sax Transformer produces invalid xml, that cannot be decoded (Problem seems to be the namespace handling).
 *
 */
@Ignore
public class EXILibTest {

    public static final AlignmentType ALIGNMENT_TYPE = AlignmentType.preCompress;

    @Test
    public void testExiLibWithSaxTransformer() throws Exception {
        final byte[] encode = encodeEXI(getDom2());
        final byte[] encodeWithTransformer = encodeEXITransformer(getDom2());

        // System.err.println(Arrays.toString(encode));
        // System.err.println(Arrays.toString(encodeWithTransformer));

        // This works fine (encoded from string)
        decodeEXI(encode);
        // Error, encoded from Dom with Transformer cannot be decoded, Exception is thrown
        //
        // either:
        // org.w3c.dom.DOMException: NAMESPACE_ERR: An attempt is made to create or change an object in a way which is incorrect with regard to namespaces.
        //
        // or:
        // java.lang.NullPointerException
        //
        // depends on GrammarOptions.addNS(go); option set
        decodeEXI(encodeWithTransformer);
    }

    private static final SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();

    public static byte[] encodeEXITransformer(final Element xml) throws Exception {
        final Transmogrifier transmogrifier = new Transmogrifier();

        transmogrifier.setAlignmentType(ALIGNMENT_TYPE);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        transmogrifier.setGrammarCache(getGrammarCache());

        transmogrifier.setOutputStream(out);

        final Transformer transformer = saxTransformerFactory.newTransformer();
        transformer.transform(new DOMSource(xml), new SAXResult(transmogrifier.getSAXTransmogrifier()));

        return out.toByteArray();
    }

    public static byte[] encodeEXI(final Element xml) throws Exception {
        final Transmogrifier transmogrifier = new Transmogrifier();

        transmogrifier.setAlignmentType(ALIGNMENT_TYPE);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        transmogrifier.setGrammarCache(getGrammarCache());

        transmogrifier.setOutputStream(out);

        transmogrifier.encode(new InputSource(new ByteArrayInputStream(toString(xml, false).getBytes())));

        out.flush();

        return out.toByteArray();
    }

    private static GrammarCache getGrammarCache() {
        short go = GrammarOptions.DEFAULT_OPTIONS;

        // This option on or off, nagasena still fails
//        go = GrammarOptions.addNS(go);

        return new GrammarCache(null, go);
    }

    public static Document decodeEXI(final byte[] input) throws Exception {

        final GrammarCache grammarCache;
        final DOMResult domResult = new DOMResult();

        try(ByteArrayInputStream in = new ByteArrayInputStream(input)) {

            final EXIReader reader = new EXIReader();

            reader.setAlignmentType(ALIGNMENT_TYPE);
            grammarCache = getGrammarCache();

            reader.setGrammarCache(grammarCache);

            final SAXTransformerFactory transformerFactory
                    = (SAXTransformerFactory) TransformerFactory.newInstance();
            final TransformerHandler handler = transformerFactory.newTransformerHandler();
            handler.setResult(domResult);

            reader.setContentHandler(handler);

            reader.parse(new InputSource(in));
        }

        return (Document) domResult.getNode();
    }

    public static Element getDom() {
        final Element dom;

        final Document d = newDocument();

        dom = d.createElement("rpc");
        dom.setAttribute("xmlns", "a.b.c");
        dom.setAttribute("message-id", "id");
        dom.appendChild(d.createElement("inner"));

        return dom;
    }

    public static Element getDom2() {
        final Element dom;

        final Document d = newDocument();

        dom = d.createElementNS("a.b.c", "rpc");
        dom.setAttribute("message-id", "id");
        dom.appendChild(d.createElement("inner"));

        return dom;
    }

    private static final DocumentBuilderFactory BUILDERFACTORY;

    static {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setCoalescing(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setIgnoringComments(true);
        BUILDERFACTORY = factory;
    }

    private static Document newDocument() {
        try {
            final DocumentBuilder builder = BUILDERFACTORY.newDocumentBuilder();
            return builder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Failed to create document", e);
        }
    }

    private static String toString(final Element xml, final boolean addXmlDeclaration) {
        try {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, addXmlDeclaration ? "no" : "yes");

            final StreamResult result = new StreamResult(new StringWriter());
            final DOMSource source = new DOMSource(xml);
            transformer.transform(source, result);

            return result.getWriter().toString();
        } catch (IllegalArgumentException | TransformerFactoryConfigurationError | TransformerException e) {
            throw new RuntimeException("Unable to serialize xml element " + xml, e);
        }
    }
}
