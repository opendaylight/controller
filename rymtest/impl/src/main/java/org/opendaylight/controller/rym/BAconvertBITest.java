/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.rym;


import com.google.common.base.Optional;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rymtest.rev200701.Level1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rymtest.rev200701.Level1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.rymtest.rev200701.Level1Key;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class BAconvertBITest extends AbstractConcurrentDataBrokerTest {

//    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.create(Top.class);
//    private static final TopLevelListKey TOP_LIST_KEY = new TopLevelListKey("foo");
//    private static final InstanceIdentifier<TopLevelList> NODE_PATH = TOP_PATH.child(TopLevelList.class, TOP_LIST_KEY);
//    private static final TopLevelList NODE = new TopLevelListBuilder().withKey(TOP_LIST_KEY).build();

    private static final Level1Key LEVEL_1_KEY = new Level1Key("L1_1", "L1_2");
    private static final InstanceIdentifier<Level1> NODE_PATH = InstanceIdentifier.builder(Level1.class, LEVEL_1_KEY).build();
    private static final Level1 NODE = new Level1Builder().withKey(LEVEL_1_KEY).setTest3("L1_3").setTest4("L1_4").build();

    public void testSubmit() throws InterruptedException, ExecutionException, TimeoutException {
        Thread.sleep(30000);
//        while (true) {
            WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
            long start = System.currentTimeMillis();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, NODE_PATH, NODE);
            System.out.println(System.currentTimeMillis() - start);
            writeTx.submit().get(5, TimeUnit.SECONDS);
//        }
//
//        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
//        Optional<TopLevelList> listNode = readTx.read(LogicalDatastoreType.OPERATIONAL, NODE_PATH).get();
//        assertTrue("List node must exists after commit", listNode.isPresent());
//        assertEquals("List node", NODE, listNode.get());

    }

    public static void main(String args[]) throws Exception {
        BAconvertBITest a = new BAconvertBITest();
        a.setup();
        a.testSubmit();
    }

}
