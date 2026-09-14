/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sample.toaster.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.DisplayString;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastOutput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.WheatBread;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;

public class OpenDaylightToasterTest extends AbstractConcurrentDataBrokerTest {
    private static final DataObjectIdentifier<Toaster> TOASTER_IID =
        DataObjectIdentifier.builder(Toaster.class).build();

    private OpendaylightToaster toaster;

    @Before
    public void setupToaster() {
        toaster = new OpendaylightToaster(getDataBroker(), mock(NotificationPublishService.class),
            mock(RpcProviderService.class));
    }

    @Test
    public void testToasterInitOnStartUp() throws Exception {
        DataBroker broker = getDataBroker();

        Optional<Toaster> optional;
        try (ReadTransaction readTx = broker.newReadOnlyTransaction()) {
            optional = readTx.read(LogicalDatastoreType.OPERATIONAL, TOASTER_IID).get();
        }
        assertNotNull(optional);
        assertTrue("Operational toaster not present", optional.isPresent());

        Toaster toasterData = optional.orElseThrow();

        assertEquals(Toaster.ToasterStatus.Up, toasterData.getToasterStatus());
        assertEquals(new DisplayString("Opendaylight"), toasterData.getToasterManufacturer());
        assertEquals(new DisplayString("Model 1 - Binding Aware"), toasterData.getToasterModelNumber());

        try (ReadTransaction readTx = broker.newReadOnlyTransaction()) {
            Boolean configToaster = readTx.exists(LogicalDatastoreType.CONFIGURATION, TOASTER_IID).get();
            assertFalse("Didn't expect config data for toaster.", configToaster);
        }
    }

    @Test
    @Ignore //ignored because it is not a test right now. Illustrative purposes only.
    public void testSomething() throws Exception {
        MakeToastInput toastInput = new MakeToastInputBuilder().setToasterDoneness(Uint32.valueOf(1))
                .setToasterToastType(WheatBread.VALUE).build();

        // NOTE: In a real test we would want to override the Thread.sleep() to
        // prevent our junit test
        // for sleeping for a second...
        Future<RpcResult<MakeToastOutput>> makeToast = toaster.invoke(toastInput);

        RpcResult<MakeToastOutput> rpcResult = makeToast.get();

        assertNotNull(rpcResult);
        assertTrue(rpcResult.isSuccessful());
        // etc
    }
}
