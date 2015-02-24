/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * FIXME: THis test should be moved to sal-binding-broker and rewriten
 * to use new DataBroker API
 */
public class ConcurrentImplicitCreateTest extends AbstractDataServiceTest {

    private static final TopLevelListKey FOO_KEY = new TopLevelListKey("foo");
    private static final TopLevelListKey BAR_KEY = new TopLevelListKey("bar");
    private static final InstanceIdentifier<Top> TOP_PATH = InstanceIdentifier.builder(Top.class).build();
    private static final InstanceIdentifier<TopLevelList> FOO_PATH = TOP_PATH.child(TopLevelList.class, FOO_KEY);
    private static final InstanceIdentifier<TopLevelList> BAR_PATH = TOP_PATH.child(TopLevelList.class, BAR_KEY);

    @Test
    public void testConcurrentCreate() throws InterruptedException, ExecutionException {

        DataModificationTransaction fooTx = baDataService.beginTransaction();
        DataModificationTransaction barTx = baDataService.beginTransaction();

        fooTx.putOperationalData(FOO_PATH, new TopLevelListBuilder().setKey(FOO_KEY).build());
        barTx.putOperationalData(BAR_PATH, new TopLevelListBuilder().setKey(BAR_KEY).build());

        Future<RpcResult<TransactionStatus>> fooFuture = fooTx.commit();
        Future<RpcResult<TransactionStatus>> barFuture = barTx.commit();

        RpcResult<TransactionStatus> fooResult = fooFuture.get();
        RpcResult<TransactionStatus> barResult = barFuture.get();

        assertTrue(fooResult.isSuccessful());
        assertTrue(barResult.isSuccessful());

        assertEquals(TransactionStatus.COMMITED, fooResult.getResult());
        assertEquals(TransactionStatus.COMMITED, barResult.getResult());

    }
}
