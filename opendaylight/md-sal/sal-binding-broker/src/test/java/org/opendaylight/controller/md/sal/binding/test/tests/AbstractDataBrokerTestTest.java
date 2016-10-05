/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.tests;

import static com.google.common.truth.Truth.assertThat;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Integration tests the AbstractDataBrokerTest.
 *
 * @author Michael Vorburger
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AbstractDataBrokerTestTest extends AbstractDataBrokerTest {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    @Test
    public void aEnsureDataBrokerIsNotNull() {
        assertThat(getDataBroker()).isNotNull();
    }

    @Test
    public void bPutSomethingIntoDataStore() throws Exception {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        Top top = new TopBuilder().build();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP_PATH, top);
        writeTx.submit().checkedGet();

        assertThat(isTopInDataStore(getDataBroker())).isTrue();
    }

    private boolean isTopInDataStore(DataBroker dataBroker) throws ReadFailedException {
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        try {
            return readTx.read(LogicalDatastoreType.CONFIGURATION, TOP_PATH).checkedGet().isPresent();
        } finally {
            readTx.close();
        }
    }

    @Test
    public void cEnsureDataStoreIsEmptyAgainInNewTest() throws ReadFailedException {
        assertThat(isTopInDataStore(getDataBroker())).isFalse();
    }

}
