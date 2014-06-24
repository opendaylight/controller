/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.SimpleNodeWrapper;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class ToCnSnWithMultipleEqualNamesTest extends YangAndXmlAndDataSchemaLoader {
    private static final String namespace = "multiple:nodes";

    @BeforeClass
    public static void initialize() {
        dataLoad("/multiple-nodes", 1, "multiple-nodes", "cont");
    }

    /**
     * If schema node is container 'foo' and corresponding data contains
     * elements with name 'foo' more then once then in phase of normalization
     * RestconfDocumentedException is raised.
     */
    @Test
    public void containerWithMultipleEqualNodeNamesTest() {
        RestconfImpl restConf = RestconfImpl.getInstance();
        ControllerContext contContext = ControllerContext.getInstance();
        contContext.setGlobalSchema(TestUtils.loadSchemaContext(modules));
        restConf.setControllerContext(contContext);
        try {
            restConf.createConfigurationData(preparePayload());
            fail("RestconfDocumentedException was excepted but wasn't thrown.");
        } catch (RestconfDocumentedException e) {
            List<RestconfError> errors = e.getErrors();
            assertEquals(1, errors.size());
            assertEquals(ErrorTag.BAD_ELEMENT, errors.get(0).getErrorTag());
        }
    }

    /**
     * Method prepare composite node structure for following XML: <cont> <cont1>
     * <lf11>value lf11</lf11> </cont1> <cont1> <lf11>value lf11</lf11> </cont1>
     * <lf1>value lf1</lf1> </cont>
     *
     * @return
     */
    private CompositeNode preparePayload() {
        CompositeNodeWrapper cont = new CompositeNodeWrapper(URI.create(namespace), "cont");

        CompositeNodeWrapper cont1_1 = new CompositeNodeWrapper(URI.create(namespace), "cont1");

        SimpleNodeWrapper lf11 = new SimpleNodeWrapper(URI.create(namespace), "lf11", "value lf11");
        cont1_1.addValue(lf11);
        cont.addValue(cont1_1);

        CompositeNodeWrapper cont1_2 = new CompositeNodeWrapper(URI.create(namespace), "cont1");
        cont1_2.addValue(lf11);
        cont.addValue(cont1_2);

        SimpleNodeWrapper lf1 = new SimpleNodeWrapper(URI.create(namespace), "lf1", "value lf1");
        cont.addValue(lf1);

        return cont;
    }
}
