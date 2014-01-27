/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;

public class CnSnToJsonIdentityrefTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/cnsn-to-json/identityref", 2, "identityref-module", "cont");
    }

    @Test
    public void identityrefToJsonTest() {
        String json = null;
        try {
            QName valueAsQname = TestUtils.buildQName("name_test", "identityref:module", "2013-12-2");
            json = TestUtils.writeCompNodeWithSchemaContextToOutput(prepareCompositeNode(valueAsQname), modules,
                    dataSchemaNode, StructuredDataToJsonProvider.INSTANCE);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
        assertNotNull(json);
        Pattern ptrn = Pattern.compile(".*\"lf1\"\\p{Space}*:\\p{Space}*\"identityref-module:name_test\".*",
                Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(json);

        assertTrue(mtch.matches());
    }

    @Test
    public void identityrefToJsonWithoutQNameTest() {
        String json = null;
        try {
            String value = "not q name value";
            json = TestUtils.writeCompNodeWithSchemaContextToOutput(prepareCompositeNode(value), modules,
                    dataSchemaNode, StructuredDataToJsonProvider.INSTANCE);
        } catch (WebApplicationException | IOException e) {
            // shouldn't end here
            assertTrue(false);
        }
        System.out.println(json);
        assertNotNull(json);
        Pattern ptrn = Pattern.compile(".*\"lf1\"\\p{Space}*:\\p{Space}*\"not q name value\".*", Pattern.DOTALL);
        Matcher mtch = ptrn.matcher(json);

        assertTrue(mtch.matches());
    }

    private CompositeNode prepareCompositeNode(Object value) {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont","identityref:module","2013-12-2"), null, null,
                ModifyAction.CREATE, null);
        MutableCompositeNode cont1 = NodeFactory.createMutableCompositeNode(TestUtils.buildQName("cont1","identityref:module","2013-12-2"), cont, null,
                ModifyAction.CREATE, null);
        cont.getChildren().add(cont1);

        MutableSimpleNode<?> lf1 = NodeFactory.createMutableSimpleNode(TestUtils.buildQName("lf1","identityref:module","2013-12-2"), cont1, value,
                ModifyAction.CREATE, null);

        cont1.getChildren().add(lf1);
        cont1.init();
        cont.init();

        return cont;
    }

}
