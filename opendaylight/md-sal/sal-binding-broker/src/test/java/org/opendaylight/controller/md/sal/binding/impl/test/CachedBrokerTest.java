/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.CachedForwardedBrokerBuilder;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.opendaylight.controller.md.sal.test.model.util.ListsBindingUtils.top;

public class CachedBrokerTest extends AbstractDataBrokerTest{

    private static final InstanceIdentifier<Top> TOP = InstanceIdentifier.create(Top.class);

    @Test
    public void cachedTest() {
        DataBroker dataBroker = CachedForwardedBrokerBuilder.create(getDataBroker(), TOP);
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, TOP, top());
        assertCommit(writeTx.submit());
    }
}
