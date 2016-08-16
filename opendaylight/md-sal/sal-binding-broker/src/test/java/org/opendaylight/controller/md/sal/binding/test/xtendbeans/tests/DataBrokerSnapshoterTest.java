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
import java.util.Objects;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.xtendbeans.DataBrokerSnapshoter;
import org.opendaylight.controller.md.sal.binding.test.xtendbeans.XtendYangBeanGenerator;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for and illustration of usage of {@link DataBrokerSnapshoter}.
 *
 * @author Michael Vorburger
 */
public class DataBrokerSnapshoterTest extends AbstractDataBrokerTest {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);
    private DataBroker dataBroker;

    @Test
    public void testDataBrokerSnapshoter() throws TransactionCommitFailedException, ReadFailedException {
        dataBroker = getDataBroker();
/* TODO
        List<TopLevelList> topLevelList = new ArrayList<>();
        TwoLevelListChanged dataObject = new TwoLevelListChangedBuilder().setTopLevelList(topLevelList).build();
        InstanceIdentifier<TwoLevelListChanged> path = InstanceIdentifier.create(TwoLevelListChanged.class);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, path, dataObject);
        writeTx.submit().checkedGet();
*/
        WriteTransaction initialTx = getDataBroker().newWriteOnlyTransaction();
        put(initialTx, OPERATIONAL, ExampleYangObjects.topEmpty());
        put(initialTx, OPERATIONAL, ExampleYangObjects.topLevelList());
        initialTx.submit().checkedGet();

        // TODO How-to use DataRoot OpendaylightMdsalListTestData instead of Top
        assertEqualBeans(null, read(OPERATIONAL, Top.class));
    }

    // TODO MOVE THIS CODE .. somewhere - not sure where, yetDataBrokerSnapshoter
    void assertEqualBeans(Object expected, DataContainer actual) {
        if (!Objects.equals(expected, actual)) {
            throw new ComparisonFailure("Expected and actual beans do not match",
                    new XtendYangBeanGenerator().getExpression(expected),
                    new XtendYangBeanGenerator().getExpression(actual));
        }
    }

    <T extends DataObject> void put(WriteTransaction tx, LogicalDatastoreType store, Map.Entry<InstanceIdentifier<T>, T> obj) {
        tx.put(OPERATIONAL, obj.getKey(), obj.getValue());
    }

    // TODO MOVE THIS CODE INTO DataBrokerSnapshoter
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
