/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Tests the IcmpDiscoveryServiceImpl class
 * 
 * @author Devin Avery
 * @author Greg Hall
 */
package org.opendaylight.controller.pingdiscovery.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sample.pingdiscovery.DeviceManager;
import org.opendaylight.controller.sample.pingdiscovery.IcmpProfileManager;
import org.opendaylight.controller.sample.pingdiscovery.impl.IcmpDiscoveryServiceImpl;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverInput;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverInputBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoverOutput;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.DiscoveryState;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileGrp;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.ProfileId;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.profiles.ProfileBuilder;
import org.opendaylight.yang.gen.v1.http.opendaylight.org.samples.icmp.rev140515.profiles.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class IcmpDiscoveryServiceImplTest {

    public IcmpDiscoveryServiceImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    private IcmpDiscoveryServiceImpl idsi = new IcmpDiscoveryServiceImpl();

    @Mock private DeviceManager mockDevMgr;
    @Mock private IcmpProfileManager mockProfMgr;


    private DiscoverInput discoverInputFull;

    private ProfileBuilder profBldr;

    private ProfileGrp profileGrp_ipList_1_1_1_1;

    /**
     * Private method to setup the input objects for testing startDiscoveryProfile
     * 
     * @return instance id of profile
     */
    private void setupDiscoveryObjects() {

        // Setup the input
        List<IpAddress> ipList = new ArrayList<IpAddress>();
        ipList.add(new IpAddress(new Ipv4Address(new String("1.1.1.1"))));

        DiscoverInputBuilder inputBuilder = new DiscoverInputBuilder() ;

        ProfileId profId = new ProfileId("mockProfileId") ;

        discoverInputFull = inputBuilder
                .setId(profId)
                .setIpList(ipList)
                .setTimeoutSeconds(1)
                .build();

        profBldr = new ProfileBuilder()
        .setId(profId)
        .setKey(new ProfileKey(profId))
        .setStatus(new DiscoveryState((short) 1))
        .setTimeoutSeconds(1);

        profileGrp_ipList_1_1_1_1 = profBldr.setIpList(ipList).build();
    }


    @Before
    public void testInit() {
        System.out.println("Test Init - " + this);

        // initialize the manager impl
        idsi.setDevMgr( mockDevMgr,  mockProfMgr) ;
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
    public void testDiscover_noPreExistingProfile_success() {

        when(mockProfMgr.startDiscoveryProfile(discoverInputFull)).thenReturn(profileGrp_ipList_1_1_1_1) ;
        when(mockDevMgr.createDevice(any(String.class))).thenReturn(true) ;
        Future<RpcResult<DiscoverOutput>> result = idsi.discover(discoverInputFull) ;
        assertNotNull( result) ;
        try {
            assertEquals(0, result.get().getErrors().size()) ;
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
