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
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

public class CnSnToXmlAndJsonInstanceIdentifierTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/instanceidentifier/yang", 4, "instance-identifier-module", "cont");
    }

    @Test
    public void saveCnSnToXmlTest() throws WebApplicationException, IOException, URISyntaxException, XMLStreamException {
        CompositeNode cnSn = prepareCnSn(createInstanceIdentifier());
        String output = TestUtils.writeCompNodeWithSchemaContextToOutput(cnSn, modules, dataSchemaNode,
                StructuredDataToXmlProvider.INSTANCE);
        //uncomment for debug
        // System.out.println(output);
        validateXmlOutput(output);

    }

    @Ignore
    @Test
    public void saveCnSnWithLeafListInstIdentifierToXmlTest() throws WebApplicationException, IOException,
            URISyntaxException, XMLStreamException {
        CompositeNode cnSn = prepareCnSn(createInstanceIdentifierWithLeafList());
        String output = TestUtils.writeCompNodeWithSchemaContextToOutput(cnSn, modules, dataSchemaNode,
                StructuredDataToXmlProvider.INSTANCE);
        //uncomment for debug
        // System.out.println(output);
        validateXmlOutputWithLeafList(output);
    }

    @Test
    public void saveCnSnToJsonTest() throws WebApplicationException, IOException, URISyntaxException {
        CompositeNode cnSn = prepareCnSn(createInstanceIdentifier());
        String output = TestUtils.writeCompNodeWithSchemaContextToOutput(cnSn, modules, dataSchemaNode,
                StructuredDataToJsonProvider.INSTANCE);
        boolean strInOutput = false;
        strInOutput = output
                .contains("\"augment-augment-module:lf111\": \"/instance-identifier-module:cont/instance-identifier-module:cont1/augment-module:lst11[augment-module:keyvalue111=\\\"value1\\\"][augment-module:keyvalue112=\\\"value2\\\"]/augment-augment-module:lf112\"");

        if (!strInOutput) {
            strInOutput = output
                    .contains("\"augment-augment-module:lf111\": \"/instance-identifier-module:cont/instance-identifier-module:cont1/augment-module:lst11[augment-module:keyvalue111='value1'][augment-module:keyvalue112='value2']/augment-augment-module:lf112\"");
        }
        //uncomment for debug
        // System.out.println(output);
        assertTrue(strInOutput);
    }


    @Test
    public void saveCnSnWithLeafListInstIdentifierToJsonTest() throws WebApplicationException, IOException,
            URISyntaxException {
        CompositeNode cnSn = prepareCnSn(createInstanceIdentifierWithLeafList());
        String output = TestUtils.writeCompNodeWithSchemaContextToOutput(cnSn, modules, dataSchemaNode,
                StructuredDataToJsonProvider.INSTANCE);
        //uncomment for debug
        // System.out.println(output);
        boolean strInOutput = false;
        strInOutput = output
                .contains("\"augment-augment-module:lf111\": \"/instance-identifier-module:cont/instance-identifier-module:cont1/augment-module-leaf-list:lflst11[.='lflst11_1']\"");
        if (!strInOutput) {
            strInOutput = output
                    .contains("\"augment-augment-module:lf111\": \"/instance-identifier-module:cont/instance-identifier-module:cont1/augment-module-leaf-list:lflst11[.=\\\"lflst11_1\\\"]\"");
        }

        assertTrue(strInOutput);
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

        assertTrue(xml.contains(instanceIdentifierValue));

    }

    private void validateXmlOutputWithLeafList(String xml) throws XMLStreamException {
        XMLInputFactory xmlInFactory = XMLInputFactory.newInstance();
        XMLEventReader eventReader;

        eventReader = xmlInFactory.createXMLEventReader(new ByteArrayInputStream(xml.getBytes()));
        String aModuleLfLstPrefix = null;
        String iiModulePrefix = null;
        while (eventReader.hasNext()) {
            XMLEvent nextEvent = eventReader.nextEvent();
            if (nextEvent.isStartElement()) {
                StartElement startElement = (StartElement) nextEvent;
                if (startElement.getName().getLocalPart().equals("lf111")) {
                    Iterator prefixes = startElement.getNamespaceContext().getPrefixes("augment:module:leaf:list");

                    while (prefixes.hasNext() && aModuleLfLstPrefix == null) {
                        String prefix = (String) prefixes.next();
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

        String instanceIdentifierValue = "/" + iiModulePrefix + ":cont/" + iiModulePrefix + ":cont1/"
                + aModuleLfLstPrefix + ":lflst11[.='lflst11_1']";

        assertTrue(xml.contains(instanceIdentifierValue));

    }

    private CompositeNode prepareCnSn(InstanceIdentifier instanceIdentifier) throws URISyntaxException {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont", "instance:identifier:module", "2014-01-17"), null, null,null,null);
        MutableCompositeNode cont1 = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont1", "instance:identifier:module", "2014-01-17"), cont, null,null,null);
        MutableCompositeNode lst11 = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("lst11", "augment:module", "2014-01-17"), cont1, null,null,null);

        MutableSimpleNode<?> lf111 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf111", "augment:augment:module", "2014-01-17"),
                lst11, instanceIdentifier,null,null);
        
        
        lst11.getChildren().add(lf111);
        lst11.init();

        cont1.getChildren().add(lst11);
        cont1.init();
        
        cont.getChildren().add(cont1);
        cont.init();

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

    private InstanceIdentifier createInstanceIdentifierWithLeafList() throws URISyntaxException {
        List<PathArgument> pathArguments = new ArrayList<>();
        pathArguments.add(new NodeIdentifier(new QName(new URI("instance:identifier:module"), "cont")));
        pathArguments.add(new NodeIdentifier(new QName(new URI("instance:identifier:module"), "cont1")));
        pathArguments.add(new NodeWithValue(new QName(new URI("augment:module:leaf:list"), "lflst11"), "lflst11_1"));

        return new InstanceIdentifier(pathArguments);
    }

}
