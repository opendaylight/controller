/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.data;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

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
    public void testConcurrentCreate() throws InterruptedException, ExecutionException, TimeoutException {

        DataBroker dataBroker = testContext.getDataBroker();
        WriteTransaction fooTx = dataBroker.newWriteOnlyTransaction();
        WriteTransaction barTx = dataBroker.newWriteOnlyTransaction();

        fooTx.put(LogicalDatastoreType.OPERATIONAL, FOO_PATH, new TopLevelListBuilder().setKey(FOO_KEY).build());
        barTx.put(LogicalDatastoreType.OPERATIONAL, BAR_PATH, new TopLevelListBuilder().setKey(BAR_KEY).build());

        fooTx.submit().get(5, TimeUnit.SECONDS);
        barTx.submit().get(5, TimeUnit.SECONDS);
    }
}
