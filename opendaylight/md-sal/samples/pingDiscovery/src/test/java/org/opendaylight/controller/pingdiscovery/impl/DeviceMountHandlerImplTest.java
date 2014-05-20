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
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sample.pingdiscovery.InventoryUtils.INVENTORY_ID;
import static org.opendaylight.controller.sample.pingdiscovery.InventoryUtils.INVENTORY_NODE;
import static org.opendaylight.controller.sample.pingdiscovery.InventoryUtils.INVENTORY_PATH;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sample.pingdiscovery.dependencies.mgrs.BundleContextDependencyManager;
import org.opendaylight.controller.sample.pingdiscovery.dependencies.mgrs.MountingServiceDependencyManager;
import org.opendaylight.controller.sample.pingdiscovery.impl.DeviceMountHandlerImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class DeviceMountHandlerImplTest {

    @Mock private MountingServiceDependencyManager mockDepMgr;
    @Mock private DataBrokerService mockDataBroker;
    @Mock private BundleContextDependencyManager mockBundleContextMgr;

    @Mock MountProvisionInstance mockMountInstance ;
    @Mock MountProvisionService mockMountProvisionService ;
    private InstanceIdentifier nodePath ;

    private String nodeIdStr = new String("ping_10.10.10.10") ;
    private DeviceMountHandlerImpl deviceMountHandlerImpl = new DeviceMountHandlerImpl() ;

    public DeviceMountHandlerImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void testInit() {
        deviceMountHandlerImpl.setDataBrokerService(mockDataBroker);
        deviceMountHandlerImpl.setDependencyMgr(mockDepMgr);
        deviceMountHandlerImpl.setBundleContextDepMgr(mockBundleContextMgr) ;
        nodePath = InstanceIdentifier
                .builder(INVENTORY_PATH)
                .nodeWithKey(INVENTORY_NODE,
                        Collections.<QName, Object> singletonMap(INVENTORY_ID, nodeIdStr))
                        .toInstance();
        when( mockDepMgr.getMountService()).thenReturn(mockMountProvisionService) ;
        when( mockMountProvisionService.createOrGetMountPoint(nodePath))
        .thenReturn(mockMountInstance);
    }
    @Test
    public void testCreateDevice() {

        deviceMountHandlerImpl.mountIcmpDataNode( nodeIdStr ) ;
    }
}
