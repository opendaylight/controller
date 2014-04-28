/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.WebApplicationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;

public class XmlAndJsonToCnSnLeafRefTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/leafref/yang", 2, "leafref-module", "cont");
    }

    @Test
    public void loadXmlToCnSn() throws WebApplicationException, IOException, URISyntaxException {
        CompositeNode cnSn = TestUtils.readInputToCnSn("/leafref/xml/xmldata.xml", XmlToCompositeNodeProvider.INSTANCE);
        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
        verifyContPredicate(cnSn, "/ns:cont/ns:lf1", "/cont/lf1", "/ns:cont/ns:lf1", "../lf1");
    }

    @Test
    public void loadJsonToCnSn() throws WebApplicationException, IOException, URISyntaxException {
        CompositeNode cnSn = TestUtils.readInputToCnSn("/leafref/json/jsondata.json",
                JsonToCompositeNodeProvider.INSTANCE);
        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
        verifyContPredicate(cnSn, "/leafref-module:cont/leafref-module:lf1", "/leafref-module:cont/leafref-module:lf1",
                "/referenced-module:cont/referenced-module:lf1", "/leafref-module:cont/leafref-module:lf1");
    }

    private void verifyContPredicate(CompositeNode cnSn, String... values) throws URISyntaxException {
        Object lf2Value = null;
        Object lf3Value = null;
        Object lf4Value = null;
        Object lf5Value = null;

        for (Node<?> node : cnSn.getValue()) {
            if (node.getNodeType().getLocalName().equals("lf2")) {
                lf2Value = ((SimpleNode<?>) node).getValue();
            } else if (node.getNodeType().getLocalName().equals("lf3")) {
                lf3Value = ((SimpleNode<?>) node).getValue();
            } else if (node.getNodeType().getLocalName().equals("lf4")) {
                lf4Value = ((SimpleNode<?>) node).getValue();
            } else if (node.getNodeType().getLocalName().equals("lf5")) {
                lf5Value = ((SimpleNode<?>) node).getValue();
            }
        }
        assertEquals(values[0], lf2Value);
        assertEquals(values[1], lf3Value);
        assertEquals(values[2], lf4Value);
        assertEquals(values[3], lf5Value);
    }

}
