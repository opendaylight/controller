/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.xtendbeans.tests;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import com.google.common.base.Optional;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.xtendbeans.AssertDataObjects;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for and illustration of usage of {@link DataBrokerSnapshoter}.
 *
 * @author Michael Vorburger
 */
public class DataBrokerSnapshoterTest extends AbstractDataBrokerTest {

    private DataBroker dataBroker;

    @Test
    public void testDataBrokerSnapshoter() throws TransactionCommitFailedException, ReadFailedException {
        dataBroker = getDataBroker();

        WriteTransaction initialTx = getDataBroker().newWriteOnlyTransaction();
        put(initialTx, OPERATIONAL, ExampleYangObjects.topEmpty());
        put(initialTx, OPERATIONAL, ExampleYangObjects.topLevelList());
        initialTx.submit().checkedGet();

        // TODO How-to read DataRoot OpendaylightMdsalListTestData instead of Top
        // assertEqualDataContainers(null, read(OPERATIONAL, Top.class));

        AssertDataObjects.assertEqualByText("import static extension org.opendaylight.controller.md.sal.binding.test.xtendbeans.XtendBuilderExtensions.operator_doubleGreaterThan\n\n"
                + "new TopBuilder >> [\n    topLevelList += #[\n        new TopLevelListBuilder\n    ]\n]", read(OPERATIONAL, Top.class));
    }

    <T extends DataObject> void put(WriteTransaction tx, LogicalDatastoreType store, Map.Entry<InstanceIdentifier<T>, T> obj) {
        tx.put(OPERATIONAL, obj.getKey(), obj.getValue());
    }

    // TODO MOVE THIS CODE INTO .. where? (No DataBrokerSnapshoter, for now.)
    <T extends ChildOf<? extends DataRoot>>
        T read(LogicalDatastoreType store, final Class<T> container) throws ReadFailedException {

        InstanceIdentifier<T> containerInstanceIdentifier = InstanceIdentifier.builder(container).build();
        try ( ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction() ) {
            Optional<T> optional = tx.read(store, containerInstanceIdentifier).checkedGet();
            if (optional.isPresent()) {
                return optional.get();
            } else {
                throw new ReadFailedException("Nothing in the " + store
                        + " datastore at: " + containerInstanceIdentifier);
            }
        }
    }

}
