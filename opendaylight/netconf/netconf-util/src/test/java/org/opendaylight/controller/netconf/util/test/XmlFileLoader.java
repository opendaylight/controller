/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.test;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class XmlFileLoader {

    public static NetconfMessage xmlFileToNetconfMessage(final String fileName) throws IOException, SAXException,
            ParserConfigurationException {
        return new NetconfMessage(xmlFileToDocument(fileName));
    }

    public static Element xmlFileToElement(final String fileName) throws IOException, SAXException,
            ParserConfigurationException {
        return xmlFileToDocument(fileName).getDocumentElement();
    }

    public static String xmlFileToString(final String fileName) throws IOException, SAXException,
            ParserConfigurationException {
        return XmlUtil.toString(xmlFileToDocument(fileName));
    }

    public static Document xmlFileToDocument(final String fileName) throws IOException, SAXException,
            ParserConfigurationException {
        try (InputStream resourceAsStream = XmlFileLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            Preconditions.checkNotNull(resourceAsStream, fileName);
            final Document doc = XmlUtil.readXmlToDocument(resourceAsStream);
            return doc;
        }
    }

    public static String fileToString(final String fileName) throws IOException {
        try (InputStream resourceAsStream = XmlFileLoader.class.getClassLoader().getResourceAsStream(fileName)) {
            Preconditions.checkNotNull(resourceAsStream);
            return new ByteSource() {
                @Override
                public InputStream openStream() {
                    return resourceAsStream;
                }
            }.asCharSource(Charsets.UTF_8).read();

        }
    }

    public static InputStream getResourceAsStream(final String fileName) {
        return XmlFileLoader.class.getClassLoader().getResourceAsStream(fileName);
    }
}
