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

import java.util.concurrent.Future;

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.DisplayString;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInput;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.MakeToastInputBuilder;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.Toaster;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev091120.WheatBread;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;

public class OpenDaylightToasterTest extends AbstractDataBrokerTest{

    private static InstanceIdentifier<Toaster> TOASTER_IID =
                        InstanceIdentifier.builder( Toaster.class ).build();
    OpendaylightToaster toaster;

    @Override
    protected void setupWithDataBroker(DataBroker dataBroker) {
        toaster = new OpendaylightToaster();
        toaster.setDataProvider( dataBroker );

        /**
         * Doesn't look like we have support for the NotificationProviderService yet, so mock it
         * for now.
         */
        NotificationProviderService mockNotification = mock( NotificationProviderService.class );
        toaster.setNotificationProvider( mockNotification );
    }

    @Test
    public void testToasterInitOnStartUp() throws Exception {
        DataBroker broker = getDataBroker();

        ReadOnlyTransaction rTx = broker.newReadOnlyTransaction();
        Optional<Toaster> optional = rTx.read( LogicalDatastoreType.OPERATIONAL, TOASTER_IID ).get();
        assertNotNull( optional );
        assertTrue( "Operational toaster not present", optional.isPresent() );

        Toaster toaster = optional.get();

        assertEquals( Toaster.ToasterStatus.Up, toaster.getToasterStatus() );
        assertEquals( new DisplayString("Opendaylight"),
                      toaster.getToasterManufacturer() );
        assertEquals( new DisplayString("Model 1 - Binding Aware"),
                      toaster.getToasterModelNumber() );

        Optional<Toaster> configToaster =
                            rTx.read( LogicalDatastoreType.CONFIGURATION, TOASTER_IID ).get();
        assertFalse( "Didn't expect config data for toaster.",
                     configToaster.isPresent() );
    }

    @Test
    @Ignore //ignored because it is not an e test right now. Illustrative purposes only.
    public void testSomething() throws Exception{
        MakeToastInput toastInput = new MakeToastInputBuilder()
                                        .setToasterDoneness( 1L )
                                        .setToasterToastType( WheatBread.class )
                                        .build();

        //NOTE: In a real test we would want to override the Thread.sleep() to prevent our junit test
        //for sleeping for a second...
        Future<RpcResult<Void>> makeToast = toaster.makeToast( toastInput );

        RpcResult<Void> rpcResult = makeToast.get();

        assertNotNull( rpcResult );
        assertTrue( rpcResult.isSuccessful() );
         //etc
    }

}
