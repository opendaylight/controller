/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.junit.BeforeClass;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;

public class CnSnToXmlAndJsonInstanceIdentifierTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() throws URISyntaxException, ReactorException, FileNotFoundException {
        dataLoad("/instanceidentifier/yang", 4, "instance-identifier-module", "cont");
    }


    private void validateXmlOutput(final String xml) throws XMLStreamException {
        final XMLInputFactory xmlInFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader;

        eventReader = xmlInFactory.createXMLEventReader(new ByteArrayInputStream(xml.getBytes()));
        String aaModulePrefix = null;
        String aModulePrefix = null;
        String iiModulePrefix = null;
        while (eventReader.hasNext()) {
            final XMLEvent nextEvent = eventReader.nextEvent();
            if (nextEvent.isStartElement()) {
                final StartElement startElement = (StartElement) nextEvent;
                if (startElement.getName().getLocalPart().equals("lf111")) {
                    final Iterator<?> prefixes = startElement.getNamespaceContext().getPrefixes("augment:augment:module");

                    while (prefixes.hasNext() && aaModulePrefix == null) {
                        final String prefix = (String) prefixes.next();
                        if (!prefix.isEmpty()) {
                            aaModulePrefix = prefix;
                        }
                    }

                    aModulePrefix = startElement.getNamespaceContext().getPrefix("augment:module");
                    iiModulePrefix = startElement.getNamespaceContext().getPrefix("instance:identifier:module");
                    break;
                }
            }
        }

        assertNotNull(aaModulePrefix);
        assertNotNull(aModulePrefix);
        assertNotNull(iiModulePrefix);

        final String instanceIdentifierValue = "/" + iiModulePrefix + ":cont/" + iiModulePrefix + ":cont1/" + aModulePrefix
                + ":lst11[" + aModulePrefix + ":keyvalue111='value1'][" + aModulePrefix + ":keyvalue112='value2']/"
                + aaModulePrefix + ":lf112";

        assertTrue(xml.contains(instanceIdentifierValue));

    }

    private void validateXmlOutputWithLeafList(final String xml) throws XMLStreamException {
        final XMLInputFactory xmlInFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader;

        eventReader = xmlInFactory.createXMLEventReader(new ByteArrayInputStream(xml.getBytes()));
        String aModuleLfLstPrefix = null;
        String iiModulePrefix = null;
        while (eventReader.hasNext()) {
            final XMLEvent nextEvent = eventReader.nextEvent();
            if (nextEvent.isStartElement()) {
                final StartElement startElement = (StartElement) nextEvent;
                if (startElement.getName().getLocalPart().equals("lf111")) {
                    final Iterator<?> prefixes = startElement.getNamespaceContext().getPrefixes("augment:module:leaf:list");

                    while (prefixes.hasNext() && aModuleLfLstPrefix == null) {
                        final String prefix = (String) prefixes.next();
                        if (!prefix.isEmpty()) {
                            aModuleLfLstPrefix = prefix;
                        }
                    }
                    iiModulePrefix = startElement.getNamespaceContext().getPrefix("instance:identifier:module");
                    break;
                }
            }
        }

        assertNotNull(aModuleLfLstPrefix);
        assertNotNull(iiModulePrefix);

        final String instanceIdentifierValue = "/" + iiModulePrefix + ":cont/" + iiModulePrefix + ":cont1/"
                + aModuleLfLstPrefix + ":lflst11[.='lflst11_1']";

        assertTrue(xml.contains(instanceIdentifierValue));

    }

    private YangInstanceIdentifier createInstanceIdentifier() throws URISyntaxException {
        final List<PathArgument> pathArguments = new ArrayList<>();
        pathArguments.add(new NodeIdentifier(new QName(new URI("instance:identifier:module"), "cont")));
        pathArguments.add(new NodeIdentifier(new QName(new URI("instance:identifier:module"), "cont1")));

        final QName qName = new QName(new URI("augment:module"), "lst11");
        final Map<QName, Object> keyValues = new HashMap<>();
        keyValues.put(new QName(new URI("augment:module"), "keyvalue111"), "value1");
        keyValues.put(new QName(new URI("augment:module"), "keyvalue112"), "value2");
        final NodeIdentifierWithPredicates nodeIdentifierWithPredicates = new NodeIdentifierWithPredicates(qName, keyValues);
        pathArguments.add(nodeIdentifierWithPredicates);

        pathArguments.add(new NodeIdentifier(new QName(new URI("augment:augment:module"), "lf112")));

        return YangInstanceIdentifier.create(pathArguments);
    }

    private YangInstanceIdentifier createInstanceIdentifierWithLeafList() throws URISyntaxException {
        final List<PathArgument> pathArguments = new ArrayList<>();
        pathArguments.add(new NodeIdentifier(new QName(new URI("instance:identifier:module"), "cont")));
        pathArguments.add(new NodeIdentifier(new QName(new URI("instance:identifier:module"), "cont1")));
        pathArguments.add(new NodeWithValue(new QName(new URI("augment:module:leaf:list"), "lflst11"), "lflst11_1"));

        return YangInstanceIdentifier.create(pathArguments);
    }

}
