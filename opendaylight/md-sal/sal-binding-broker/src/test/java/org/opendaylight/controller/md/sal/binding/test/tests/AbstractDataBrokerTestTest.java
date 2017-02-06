/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.TOP_FOO_KEY;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.path;
import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.topLevelList;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.TreeComplexUsesAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.augment.rev140709.complex.from.grouping.ContainerWithUsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Integration tests the AbstractDataBrokerTest.
 *
 * @author Michael Vorburger
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AbstractDataBrokerTestTest extends AbstractConcurrentDataBrokerTest {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);

    @Before
    public void before() {
        assertThat(getDataBroker()).isNotNull();
    }

    @Test
    public void aEnsureDataBrokerIsNotNull() {
        assertThat(getDataBroker()).isNotNull();
    }

    @Test
    public void bPutSomethingIntoDataStore() throws Exception {
        writeInitialState();
        assertThat(isTopInDataStore()).isTrue();
    }

    @Test
    public void cEnsureDataStoreIsEmptyAgainInNewTest() throws ReadFailedException {
        assertThat(isTopInDataStore()).isFalse();
    }

    // copy/pasted from Bug1125RegressionTest.writeInitialState()
    private void writeInitialState() throws TransactionCommitFailedException {
        WriteTransaction initialTx = getDataBroker().newWriteOnlyTransaction();
        initialTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, new TopBuilder().build());
        TreeComplexUsesAugment fooAugment = new TreeComplexUsesAugmentBuilder()
                .setContainerWithUses(new ContainerWithUsesBuilder().setLeafFromGrouping("foo").build()).build();
        initialTx.put(LogicalDatastoreType.OPERATIONAL, path(TOP_FOO_KEY), topLevelList(TOP_FOO_KEY, fooAugment));
        initialTx.submit().checkedGet();
    }

    private boolean isTopInDataStore() throws ReadFailedException {
        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        try {
            return readTx.read(LogicalDatastoreType.OPERATIONAL, TOP_PATH).checkedGet().isPresent();
        } finally {
            readTx.close();
        }
    }

}
