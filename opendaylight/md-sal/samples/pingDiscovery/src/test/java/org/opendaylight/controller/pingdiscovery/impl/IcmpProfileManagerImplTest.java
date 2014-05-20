/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Tests the IcmpProfileManagerImpl class
 * 
 * @author Devin Avery
 * @author Greg Hall
 */
package org.opendaylight.controller.pingdiscovery.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sample.pingdiscovery.impl.IcmpProfileManagerImpl;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverInput;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverInputBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoveryState;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileGrp;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileId;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.profiles.Profile;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.profiles.ProfileBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.profiles.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class IcmpProfileManagerImplTest {

    public IcmpProfileManagerImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    private IcmpProfileManagerImpl ipmi = new IcmpProfileManagerImpl();

    @Mock
    private DataProviderService mockDataProviderService;
    @Mock
    private NotificationProviderService mockNotificationProviderService;
    @Mock
    private DataModificationTransaction dataModTxn;
    @Mock
    private NotificationProviderService mockNotificationServiceDependency;

    private RpcResult<TransactionStatus> resultCompNode;
    // TransactionStatus mockTransactionStatus = new TransactionStatus() ;

    private ListenableFuture<RpcResult<TransactionStatus>> futureCompNode;

    private DiscoverInput discoverInputFull;
    private DiscoverInput discoverInputIdOnly;

    private ProfileBuilder profBldr;

    private ProfileGrp profileGrp_IdOnly;
    private ProfileGrp profileGrp_ipList_1_1_1_1;
    private ProfileGrp profileGrp_ipList_2_2_2_2;

    /**
     * Private method to setup the input objects for testing startDiscoveryProfile
     * 
     * @return instance id of profile
     */
    private InstanceIdentifier<Profile> setupStartDiscoveryObjects() {
        resultCompNode = Rpcs.getRpcResult(true, TransactionStatus.SUBMITED,
                Collections.<RpcError> emptySet());
        futureCompNode = Futures.immediateFuture(resultCompNode);

        // Setup the input
        List<IpAddress> ipList1 = new ArrayList<IpAddress>();
        ipList1.add(new IpAddress(new Ipv4Address(new String("1.1.1.1"))));
        List<IpAddress> ipList2 = new ArrayList<IpAddress>();
        ipList2.add(new IpAddress(new Ipv4Address(new String("2.2.2.2"))));
        List<IpAddress> emptyIpList = new ArrayList<IpAddress>();

        DiscoverInputBuilder inputBuilder = new DiscoverInputBuilder() ;

        ProfileId profId = new ProfileId("mockProfileId") ;
        discoverInputIdOnly = inputBuilder
                .setId(profId)
                .setIpList(emptyIpList)
                .build();

        discoverInputFull = inputBuilder
                .setIpList(ipList2)
                .setTimeoutSeconds(1)
                .build();

        profBldr = new ProfileBuilder()
        .setId(profId)
        .setKey(new ProfileKey(profId))
        .setStatus(new DiscoveryState((short) 1))
        .setTimeoutSeconds(1);

        profileGrp_IdOnly = profBldr.setIpList(ipList1).build();
        profileGrp_ipList_1_1_1_1 = profBldr.setIpList(emptyIpList).build();
        profileGrp_ipList_2_2_2_2 = profBldr.setIpList(ipList2).build();

        InstanceIdentifier<Profile> readPath = ipmi.createPath(profId);
        return readPath;
    }


    @Before
    public void testInit() {
        System.out.println("Test Init - " + this);

        // initialize the manager impl
        ipmi.setDataBrokerService(mockDataProviderService);
        ipmi.setNotificationProvider(mockNotificationServiceDependency);
    }

    /**
     * Test the happy path through startDiscoveryProfile where no profile yet
     * exists, and a complete profile is passed through the RPC.
     * 
     * The key assumption of this method is that each scalar leaf value of the
     * input replaces the existing value in the datastore. The list leaf items
     * are "merged" to yield the union of input and existing. This test doesn't
     * mock this important semantic.
     * 
     * MD-SAL doesn't declare a specific exception from most methods, and
     * therefore error handling is a general case. startDiscoveryProfile method
     * returns true if no exception occurs.
     */
    @Test
    public void testStartDiscoveryProfile_noPreExistingProfile_success() {

        System.out
        .println("testStartDiscoveryProfile_noPreExistingProfile_success - "
                + this);

        when(mockDataProviderService.beginTransaction()).thenReturn(dataModTxn);

        InstanceIdentifier<Profile> readPath = setupStartDiscoveryObjects();

        when(dataModTxn.commit()).thenReturn(futureCompNode);

        // The datastore should first return null, then next return the commited
        // merged profile
        when(mockDataProviderService.readConfigurationData(readPath))
        .thenReturn(null, profileGrp_ipList_2_2_2_2);

        ProfileGrp startReturn = ipmi.startDiscoveryProfile(discoverInputFull);

        // a failed commit via exception should result in a null return:
        assertNotNull(startReturn);
        assertEquals(startReturn, profileGrp_ipList_2_2_2_2);
    }

    /**
     * Test the failed commit path through startDiscoveryProfile.
     * 
     * The purpose of this method is to ensure that each value of the input
     * replaces the existing value in the datastore. With MD-SAL's datastores,
     * the list leaf items are "merged" to yield the union of input and
     * existing. This test doesn't mock this important semantic.
     * 
     * A danger exists where if the commit of the removeExistingProfile()
     * succeeds but the follow commit of the update fails, the end result of the
     * RPC will be deleting the existing profile. To solve this, a way of
     * replacing the existing list in a single transaction is required of
     * MD-SAL's datastores.
     * 
     * MD-SAL doesn't declare a specific exception from most methods, and
     * therefore error handling is a general case. startDiscoveryProfile method
     * returns true if no exception occurs.
     */
    @Test
    public void testStartDiscoveryProfile_noPreExisting_commitException() {
        System.out
        .println("testStartDiscoveryProfile_noPreExistingcommitException - "
                + this);

        InstanceIdentifier<Profile> readPath = setupStartDiscoveryObjects();

        when(mockDataProviderService.beginTransaction()).thenReturn(dataModTxn);

        // The datastore should first return null, and won't be called a second
        // time
        when(mockDataProviderService.readConfigurationData(readPath))
        .thenReturn(null);
        when(dataModTxn.commit()).thenReturn(null);

        ProfileGrp startReturn = ipmi.startDiscoveryProfile(discoverInputFull);

        // a failed commit via exception should result in a null return:
        assertNull(startReturn);
    }

    /**
     * Test the happy path through startDiscoveryProfile with a preExisting
     * profile, and only the profile id is passed throught the RPC.  Note that
     * it's possible to create the profile first via an http POST:
     * 
     *  http://controllerip:8080/restconf/config/icmp-discovery:profiles
     *
{
  "icmp-discovery:profile": [
      {
            "status": 0,
            "ip-list": [
                "192.168.1.28",
                "192.168.1.30",
                "192.168.1.32"
            ],
            "timeout-seconds": 1,
            "id": "testProfile1"
        }
    ]
}
     * 
     * 
     * MD-SAL doesn't declare a specific exception from most methods, and
     * therefore error handling is a general case. startDiscoveryProfile method
     * returns true if no exception occurs.
     */
    @Test
    public void testStartDiscoveryProfile_inputIdOnly_success() {

        System.out
        .println("testStartDiscoveryProfile_inputIdOnly_success - "
                + this);

        when(mockDataProviderService.beginTransaction()).thenReturn(dataModTxn);

        InstanceIdentifier<Profile> readPath = setupStartDiscoveryObjects();

        when(dataModTxn.commit()).thenReturn(futureCompNode);

        when(mockDataProviderService.readConfigurationData(readPath))
        .thenReturn(profileGrp_ipList_1_1_1_1,
                profileGrp_ipList_1_1_1_1);

        ProfileGrp startReturn = ipmi
                .startDiscoveryProfile(discoverInputIdOnly);

        // a failed commit via exception should result in a null return:
        assertNotNull(startReturn);
        assertEquals(startReturn, profileGrp_ipList_1_1_1_1);
    }
    /**
     * Test the happy path through startDiscoveryProfile with a preExisting
     * profile and a new ip list in the input to replace the existing list.
     * 
     * The key assumption of this method is that each scalar leaf value of the
     * input replaces the existing value in the datastore. The list leaf items
     * are "merged" to yield the union of input and existing. This test doesn't
     * mock this important semantic.
     * 
     * MD-SAL doesn't declare a specific exception from most methods, and
     * therefore error handling is a general case. startDiscoveryProfile method
     * returns true if no exception occurs.
     */
    @Test
    public void testStartDiscoveryProfile_fullInput_success() {

        System.out
        .println("testStartDiscoveryProfile_inputIdOnly_success - "
                + this);

        when(mockDataProviderService.beginTransaction()).thenReturn(dataModTxn);

        InstanceIdentifier<Profile> readPath = setupStartDiscoveryObjects();

        when(dataModTxn.commit()).thenReturn(futureCompNode);

        when(mockDataProviderService.readConfigurationData(readPath))
        .thenReturn(profileGrp_ipList_1_1_1_1,
                profileGrp_ipList_2_2_2_2);

        ProfileGrp startReturn = ipmi
                .startDiscoveryProfile(discoverInputFull);

        // a failed commit via exception should result in a null return:
        assertNotNull(startReturn);

        assertEquals(startReturn, profileGrp_ipList_2_2_2_2);
    }

    @Test
    public void testFinishDiscoveryProfile_success() {
        ipmi.finishDiscoveryProfile(profileGrp_IdOnly);
    }
}
