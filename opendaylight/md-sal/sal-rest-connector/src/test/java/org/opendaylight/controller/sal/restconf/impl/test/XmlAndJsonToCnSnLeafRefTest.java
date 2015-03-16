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
import org.opendaylight.yangtools.yang.common.QName;

public class XmlAndJsonToCnSnLeafRefTest extends YangAndXmlAndDataSchemaLoader {

    final QName refContQName = QName.create("referenced:module", "2014-04-17", "cont");
    final QName refLf1QName = QName.create(refContQName, "lf1");
    final QName contQName = QName.create("leafref:module", "2014-04-17", "cont");
    final QName lf1QName = QName.create(contQName, "lf1");
    final QName lf2QName = QName.create(contQName, "lf2");
    final QName lf3QName = QName.create(contQName, "lf3");

    @BeforeClass
    public static void initialize() {
        dataLoad("/leafref/yang", 2, "leafref-module", "cont");
    }

    @Test
    public void loadXmlToCnSn() throws WebApplicationException, IOException, URISyntaxException {
//        Node<?> node = TestUtils.readInputToCnSn("/leafref/xml/xmldata.xml", XmlToCompositeNodeProvider.INSTANCE);
//
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode cnSn = (CompositeNode)node;
//
//        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
//
//        verifyContPredicate(cnSn, "lf4", YangInstanceIdentifier.builder().node(refContQName).node(refLf1QName).build());
//        verifyContPredicate(cnSn, "lf2", YangInstanceIdentifier.builder().node(contQName).node(lf1QName).build());
//        verifyContPredicate(cnSn, "lf3", YangInstanceIdentifier.builder().node(contQName).node(lf2QName).build());
//        verifyContPredicate(cnSn, "lf5", YangInstanceIdentifier.builder().node(contQName).node(lf3QName).build());
    }

    @Test
    public void loadJsonToCnSn() throws WebApplicationException, IOException, URISyntaxException {
//        Node<?> node = TestUtils.readInputToCnSn("/leafref/json/jsondata.json",
//                JsonToCompositeNodeProvider.INSTANCE);
//        assertTrue(node instanceof CompositeNode);
//        CompositeNode cnSn = (CompositeNode)node;
//
//        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
//
//        verifyContPredicate(cnSn, "lf4", YangInstanceIdentifier.builder().node(refContQName).node(refLf1QName).build());
//        verifyContPredicate(cnSn, "lf2", YangInstanceIdentifier.builder().node(contQName).node(lf1QName).build());
//        verifyContPredicate(cnSn, "lf3", YangInstanceIdentifier.builder().node(contQName).node(lf2QName).build());
//        verifyContPredicate(cnSn, "lf5", YangInstanceIdentifier.builder().node(contQName).node(lf3QName).build());
    }

//    private void verifyContPredicate(CompositeNode cnSn, String leafName, Object value) throws URISyntaxException {
//        Object parsed = null;
//
//        for (final Node<?> node : cnSn.getValue()) {
//            if (node.getNodeType().getLocalName().equals(leafName)) {
//                parsed = node.getValue();
//            }
//        }
//
//        assertEquals(value, parsed);
//    }

}
