/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TopBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class WriteTransactionTest extends AbstractDataBrokerTest {

    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);
    private static final TopLevelListKey TOP_LIST_KEY = new TopLevelListKey("foo");
    private static final InstanceIdentifier<TopLevelList> NODE_PATH = TOP_PATH.child(TopLevelList.class, TOP_LIST_KEY);

    @Test
    public void test() throws InterruptedException, ExecutionException {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, TOP_PATH, new TopBuilder().build());
        writeTx.put(LogicalDatastoreType.OPERATIONAL, NODE_PATH, new TopLevelListBuilder().setKey(TOP_LIST_KEY).build());
        assertEquals(TransactionStatus.COMMITED, writeTx.commit().get().getResult());
    }

}
