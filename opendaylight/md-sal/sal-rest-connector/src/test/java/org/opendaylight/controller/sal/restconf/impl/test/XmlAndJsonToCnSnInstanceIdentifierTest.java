/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;

public class XmlAndJsonToCnSnInstanceIdentifierTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialize() {
        dataLoad("/instanceidentifier/yang", 3, "instance-identifier-module", "cont");
    }

    @Test
    public void loadXmlToCnSn() throws WebApplicationException, IOException, URISyntaxException {
        CompositeNode cnSn = TestUtils.readInputToCnSn("/instanceidentifier/xml/xmldata.xml",
                XmlToCompositeNodeProvider.INSTANCE);
        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
        verify(cnSn);
    }

    @Test
    public void loadJsonToCnSn() throws WebApplicationException, IOException, URISyntaxException {
        CompositeNode cnSn = TestUtils.readInputToCnSn("/instanceidentifier/json/jsondata.json",
                JsonToCompositeNodeProvider.INSTANCE);
        TestUtils.normalizeCompositeNode(cnSn, modules, schemaNodePath);
        verify(cnSn);
    }

    private void verify(CompositeNode cnSn) throws URISyntaxException {
        SimpleNode<?> lf111 = getSnWithInstanceIdentifier(cnSn);
        Object value = lf111.getValue();
        assertTrue(value instanceof InstanceIdentifier);

        InstanceIdentifier instanceIdentifier = (InstanceIdentifier) value;
        List<PathArgument> pathArguments = instanceIdentifier.getPath();
        assertEquals(4, pathArguments.size());
        String revisionDate = "2014-01-17";
        assertEquals(TestUtils.buildQName("cont", "instance:identifier:module", revisionDate), pathArguments.get(0)
                .getNodeType());
        assertEquals(TestUtils.buildQName("cont1", "instance:identifier:module", revisionDate), pathArguments.get(1)
                .getNodeType());
        assertEquals(TestUtils.buildQName("lst11", "augment:module", revisionDate), pathArguments.get(2).getNodeType());
        assertEquals(TestUtils.buildQName("lf112", "augment:augment:module", revisionDate), pathArguments.get(3)
                .getNodeType());

        assertTrue(pathArguments.get(2) instanceof NodeIdentifierWithPredicates);
        Map<QName, Object> predicates = ((NodeIdentifierWithPredicates) pathArguments.get(2)).getKeyValues();
        assertEquals(2, predicates.size());
        assertEquals("value1", predicates.get(TestUtils.buildQName("keyvalue111", "augment:module", revisionDate)));
        assertEquals("value2", predicates.get(TestUtils.buildQName("keyvalue112", "augment:module", revisionDate)));
    }

    private SimpleNode<?> getSnWithInstanceIdentifier(CompositeNode cnSn) throws URISyntaxException {
        CompositeNode cont1 = cnSn.getFirstCompositeByName(TestUtils.buildQName("cont1", "instance:identifier:module",
                "2014-01-17"));
        assertNotNull(cont1);
        CompositeNode lst11 = cont1.getFirstCompositeByName(TestUtils.buildQName("lst11", "augment:module",
                "2014-01-17"));
        assertNotNull(lst11);
        SimpleNode<?> lf111 = lst11.getFirstSimpleByName(TestUtils.buildQName("lf111", "augment:augment:module",
                "2014-01-17"));
        assertNotNull(lf111);
        return lf111;
    }

}
