/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.tests;

import org.junit.Test;
import org.opendaylight.controller.blueprint.ext.DataStoreAppConfigDefaultXMLReader;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Example unit test using the {@link DataStoreAppConfigDefaultXMLReader}.
 *
 * @author Michael Vorburger.ch
 */
public class DataStoreAppConfigDefaultXMLReaderTest extends AbstractConcurrentDataBrokerTest {

    DataStoreAppConfigDefaultXMLReader reader;

    @Override
    protected void setupWithSchema(SchemaContext context) {
        reader = new DataStoreAppConfigDefaultXMLReader(getClass().getName(), "opendaylight-sal-test-store-config.xml",
                getDataBrokerTestCustomizer().getSchemaService(),
                getDataBrokerTestCustomizer().getBindingToNormalized(), null, null);
    }

    @Test
    public void testConfigXML() {

    }

}
