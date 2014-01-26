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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;

public class CnSnToXmlAndJsonInstanceIdentifierTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/instanceidentifier/yang", 3, "instance-identifier-module", "cont");
    }

    @Test
    public void saveCnSnToXml() throws WebApplicationException, IOException, URISyntaxException, XMLStreamException {
        CompositeNode cnSn = prepareCnSn();
        String output = TestUtils.writeCompNodeWithSchemaContextToOutput(cnSn, modules, dataSchemaNode,
                StructuredDataToXmlProvider.INSTANCE);
        validateXmlOutput(output);
        // System.out.println(output);

    }

    @Test
    public void saveCnSnToJson() throws WebApplicationException, IOException, URISyntaxException {
        CompositeNode cnSn = prepareCnSn();
        String output = TestUtils.writeCompNodeWithSchemaContextToOutput(cnSn, modules, dataSchemaNode,
                StructuredDataToJsonProvider.INSTANCE);
        assertTrue(output
                .contains("\"augment-augment-module:lf111\": \"/instance-identifier-module:cont/instance-identifier-module:cont1/augment-module:lst11[augment-module:keyvalue111=\\\"value1\\\"][augment-module:keyvalue112=\\\"value2\\\"]/augment-augment-module:lf112\""));
        // System.out.println(output);
    }

    private void validateXmlOutput(String xml) throws XMLStreamException {
        XMLInputFactory xmlInFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader;

        eventReader = xmlInFactory.createXMLEventReader(new ByteArrayInputStream(xml.getBytes()));
        String aaModulePrefix = null;
        String aModulePrefix = null;
        String iiModulePrefix = null;
        while (eventReader.hasNext()) {
            XMLEvent nextEvent = eventReader.nextEvent();
            if (nextEvent.isStartElement()) {
                StartElement startElement = (StartElement) nextEvent;
                if (startElement.getName().getLocalPart().equals("lf111")) {
                    Iterator prefixes = startElement.getNamespaceContext().getPrefixes("augment:augment:module");

                    while (prefixes.hasNext() && aaModulePrefix == null) {
                        String prefix = (String) prefixes.next();
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

        String instanceIdentifierValue = "/" + iiModulePrefix + ":cont/" + iiModulePrefix + ":cont1/" + aModulePrefix
                + ":lst11[" + aModulePrefix + ":keyvalue111='value1'][" + aModulePrefix + ":keyvalue112='value2']/"
                + aaModulePrefix + ":lf112";

//        System.out.println(xml);
        assertTrue(xml.contains(instanceIdentifierValue));

    }

    private CompositeNode prepareCnSn() throws URISyntaxException {
        CompositeNodeWrapper cont = new CompositeNodeWrapper(new URI("instance:identifier:module"), "cont");
        CompositeNodeWrapper cont1 = new CompositeNodeWrapper(new URI("instance:identifier:module"), "cont1");
        CompositeNodeWrapper lst11 = new CompositeNodeWrapper(new URI("augment:module"), "lst11");
        InstanceIdentifier instanceIdentifier = createInstanceIdentifier();
        SimpleNodeWrapper lf111 = new SimpleNodeWrapper(new URI("augment:augment:module"), "lf111", instanceIdentifier);

        lst11.addValue(lf111);
        cont1.addValue(lst11);
        cont.addValue(cont1);

        return cont;
    }

    private InstanceIdentifier createInstanceIdentifier() throws URISyntaxException {
        List<PathArgument> pathArguments = new ArrayList<>();
        pathArguments.add(new NodeIdentifier(new QName(new URI("instance:identifier:module"), "cont")));
        pathArguments.add(new NodeIdentifier(new QName(new URI("instance:identifier:module"), "cont1")));

        QName qName = new QName(new URI("augment:module"), "lst11");
        Map<QName, Object> keyValues = new HashMap<>();
        keyValues.put(new QName(new URI("augment:module"), "keyvalue111"), "value1");
        keyValues.put(new QName(new URI("augment:module"), "keyvalue112"), "value2");
        NodeIdentifierWithPredicates nodeIdentifierWithPredicates = new NodeIdentifierWithPredicates(qName, keyValues);
        pathArguments.add(nodeIdentifierWithPredicates);

        pathArguments.add(new NodeIdentifier(new QName(new URI("augment:augment:module"), "lf112")));

        return new InstanceIdentifier(pathArguments);
    }

}
