/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.opendaylight.controller.blueprint.ext.DataStoreAppConfigDefaultXMLReader;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.Lists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedList;

/**
 * Example unit test using the {@link DataStoreAppConfigDefaultXMLReader}.
 *
 * @author Michael Vorburger.ch
 */
public class DataStoreAppConfigDefaultXMLReaderTest extends AbstractConcurrentDataBrokerTest {
    @Test
    public void testConfigXML() throws Exception {
        Lists lists = new DataStoreAppConfigDefaultXMLReader<>(getClass(), "/opendaylight-sal-test-store-config.xml",
            getDataBrokerTestCustomizer().getSchemaService(),
            getDataBrokerTestCustomizer().getAdapterContext().currentSerializer(), Lists.class)
            .createDefaultInstance();

        UnorderedList element = lists.getUnorderedContainer().getUnorderedList().values().iterator().next();
        assertEquals("someName", element.getName());
        assertEquals("someValue", element.getValue());
    }

    @Test
    public void testBadXMLName() throws Exception {
        final var reader = new DataStoreAppConfigDefaultXMLReader<>(getClass(), "/badname.xml",
            getDataBrokerTestCustomizer().getSchemaService(),
            getDataBrokerTestCustomizer().getAdapterContext().currentSerializer(), Lists.class);

        final String message = assertThrows(IllegalArgumentException.class, reader::createDefaultInstance).getMessage();
        assertEquals("resource /badname.xml relative to " + DataStoreAppConfigDefaultXMLReaderTest.class.getName()
            + " not found.", message);
    }
}
