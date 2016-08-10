/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api.testutils;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.exception;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TestDataBrokerTest {

    @Test public void simpleInTx() throws Exception {
        DataBroker db = TestDataBroker.newTestDataBroker();
        TestDataObject writtenDataObject = newDO();
        ReadWriteTransaction tx = db.newReadWriteTransaction();
        tx.put(CONFIGURATION, newID(), writtenDataObject);
        assertEquals(writtenDataObject, tx.read(CONFIGURATION, newID()).get().get());
    }

    @Test public void simpleSubmitWriteNewReadTx() throws Exception {
        DataBroker db = TestDataBroker.newTestDataBroker();
        TestDataObject writtenDataObject = newDO();
        WriteTransaction writeTx = db.newWriteOnlyTransaction();
        writeTx.put(CONFIGURATION, newID(), writtenDataObject);
        writeTx.submit();
        ReadOnlyTransaction readTx = db.newReadOnlyTransaction();
        assertEquals(writtenDataObject, readTx.read(CONFIGURATION, newID()).get().get());
    }

    private abstract static class TestDataObject implements DataObject { }

    private InstanceIdentifier<TestDataObject> newID() {
        return InstanceIdentifier.create(TestDataObject.class);
    }

    private TestDataObject newDO() {
        return Mockito.mock(TestDataObject.class, exception());
    }

}
