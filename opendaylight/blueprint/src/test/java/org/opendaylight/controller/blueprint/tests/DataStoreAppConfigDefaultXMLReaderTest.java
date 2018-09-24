/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.tests;

import static com.google.common.truth.Truth.assertThat;

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
        Lists lists = new DataStoreAppConfigDefaultXMLReader<>(
                getClass(), "/opendaylight-sal-test-store-config.xml",
                getDataBrokerTestCustomizer().getSchemaService(),
                getDataBrokerTestCustomizer().getBindingToNormalized(),
                Lists.class).createDefaultInstance();

        UnorderedList element = lists.getUnorderedContainer().getUnorderedList().get(0);
        assertThat(element.getName()).isEqualTo("someName");
        assertThat(element.getValue()).isEqualTo("someValue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadXMLName() throws Exception {
        new DataStoreAppConfigDefaultXMLReader<>(
                getClass(), "/badname.xml",
                getDataBrokerTestCustomizer().getSchemaService(),
                getDataBrokerTestCustomizer().getBindingToNormalized(),
                Lists.class).createDefaultInstance();
    }
}
