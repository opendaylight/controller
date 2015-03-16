/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.ws.rs.WebApplicationException;
import org.junit.BeforeClass;
import org.junit.Test;

public class XmlAndJsonToCnSnInstanceIdentifierTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/instanceidentifier/yang", 4, "instance-identifier-module", "cont");
    }

    @Test
    public void loadXmlToCnSn() throws WebApplicationException, IOException, URISyntaxException {
//        Node<?> node = TestUtils.readInputToCnSn("/instanceidentifier/xml/xmldata.xml",
//                XmlToCompositeNodeProvider.INSTANCE);
//
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode cnSn = (CompositeNode)node;
//        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
//        verifyListPredicate(cnSn);
    }

    @Test
    public void loadXmlLeafListToCnSn() throws WebApplicationException, IOException, URISyntaxException {
//        Node<?> node = TestUtils.readInputToCnSn("/instanceidentifier/xml/xmldata_leaf_list.xml",
//                XmlToCompositeNodeProvider.INSTANCE);
//
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode cnSn = (CompositeNode)node;
//        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
//        verifyLeafListPredicate(cnSn);
    }

    @Test
    public void loadJsonToCnSn() throws WebApplicationException, IOException, URISyntaxException {
//        Node<?> node = TestUtils.readInputToCnSn("/instanceidentifier/json/jsondata.json",
//                JsonToCompositeNodeProvider.INSTANCE);
//
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode cnSn = (CompositeNode)node;
//        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
//        verifyListPredicate(cnSn);
    }

    @Test
    public void loadJsonLeafListToCnSn() throws WebApplicationException, IOException, URISyntaxException {
//        Node<?> node = TestUtils.readInputToCnSn("/instanceidentifier/json/jsondata_leaf_list.json",
//                JsonToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode cnSn = (CompositeNode)node;
//
//        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
//        verifyLeafListPredicate(cnSn);
    }

//    private void verifyLeafListPredicate(final CompositeNode cnSn) throws URISyntaxException {
//        SimpleNode<?> lf11 = getSnWithInstanceIdentifierWhenLeafList(cnSn);
//        Object value = lf11.getValue();
//        assertTrue(value instanceof YangInstanceIdentifier);
//
//        YangInstanceIdentifier instanceIdentifier = (YangInstanceIdentifier) value;
//        Iterator<PathArgument> it = instanceIdentifier.getPathArguments().iterator();
//        String revisionDate = "2014-01-17";
//
//        assertEquals(TestUtils.buildQName("cont", "instance:identifier:module", revisionDate), it.next().getNodeType());
//        assertEquals(TestUtils.buildQName("cont1", "instance:identifier:module", revisionDate), it.next().getNodeType());
//
//        PathArgument arg = it.next();
//        assertFalse(it.hasNext());
//        assertEquals(TestUtils.buildQName("lflst11", "augment:module:leaf:list", "2014-01-27"), arg.getNodeType());
//
//        assertTrue(arg instanceof NodeWithValue);
//        assertEquals("lflst11_1", ((NodeWithValue) arg).getValue());
//
//    }

//    private void verifyListPredicate(final CompositeNode cnSn) throws URISyntaxException {
//        SimpleNode<?> lf111 = getSnWithInstanceIdentifierWhenList(cnSn);
//        Object value = lf111.getValue();
//        assertTrue(value instanceof YangInstanceIdentifier);
//
//        YangInstanceIdentifier instanceIdentifier = (YangInstanceIdentifier) value;
//        Iterator<PathArgument> it = instanceIdentifier.getPathArguments().iterator();
//        String revisionDate = "2014-01-17";
//        assertEquals(TestUtils.buildQName("cont", "instance:identifier:module", revisionDate), it.next().getNodeType());
//        assertEquals(TestUtils.buildQName("cont1", "instance:identifier:module", revisionDate), it.next().getNodeType());
//
//        PathArgument arg = it.next();
//        assertEquals(TestUtils.buildQName("lst11", "augment:module", revisionDate), arg.getNodeType());
//        assertEquals(TestUtils.buildQName("lf112", "augment:augment:module", revisionDate), it.next().getNodeType());
//        assertFalse(it.hasNext());
//
//        assertTrue(arg instanceof NodeIdentifierWithPredicates);
//        Map<QName, Object> predicates = ((NodeIdentifierWithPredicates) arg).getKeyValues();
//        assertEquals(2, predicates.size());
//        assertEquals("value1", predicates.get(TestUtils.buildQName("keyvalue111", "augment:module", revisionDate)));
//        assertEquals("value2", predicates.get(TestUtils.buildQName("keyvalue112", "augment:module", revisionDate)));
//    }

//    private SimpleNode<?> getSnWithInstanceIdentifierWhenList(final CompositeNode cnSn) throws URISyntaxException {
//        CompositeNode cont1 = cnSn.getFirstCompositeByName(TestUtils.buildQName("cont1", "instance:identifier:module",
//                "2014-01-17"));
//        assertNotNull(cont1);
//        CompositeNode lst11 = cont1.getFirstCompositeByName(TestUtils.buildQName("lst11", "augment:module",
//                "2014-01-17"));
//        assertNotNull(lst11);
//        SimpleNode<?> lf111 = lst11.getFirstSimpleByName(TestUtils.buildQName("lf111", "augment:augment:module",
//                "2014-01-17"));
//        assertNotNull(lf111);
//        return lf111;
//    }

//    private SimpleNode<?> getSnWithInstanceIdentifierWhenLeafList(final CompositeNode cnSn) throws URISyntaxException {
//        CompositeNode cont1 = cnSn.getFirstCompositeByName(TestUtils.buildQName("cont1", "instance:identifier:module",
//                "2014-01-17"));
//        assertNotNull(cont1);
//        SimpleNode<?> lf11 = cont1.getFirstSimpleByName(TestUtils.buildQName("lf11", "augment:module:leaf:list",
//                "2014-01-27"));
//        assertNotNull(lf11);
//        return lf11;
//    }

}
