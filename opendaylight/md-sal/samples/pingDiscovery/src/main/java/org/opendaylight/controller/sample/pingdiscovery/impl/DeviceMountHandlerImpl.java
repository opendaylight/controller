/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.sample.pingdiscovery.impl;

import static org.opendaylight.controller.sample.pingdiscovery.InventoryUtils.INVENTORY_ID;
import static org.opendaylight.controller.sample.pingdiscovery.InventoryUtils.INVENTORY_NODE;
import static org.opendaylight.controller.sample.pingdiscovery.InventoryUtils.INVENTORY_PATH;

import java.util.Collections;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sample.pingdiscovery.DependencyManager;
import org.opendaylight.controller.sample.pingdiscovery.DeviceMountHandler;
import org.opendaylight.controller.sample.pingdiscovery.util.AutoCloseableManager;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class DeviceMountHandlerImpl implements DeviceMountHandler , AutoCloseable{

    private final InstanceIdentifier ROOT_PATH = InstanceIdentifier.builder().build();
    private final AutoCloseableManager closeables = new AutoCloseableManager();
    private DependencyManager depMgr;
    private DataBrokerService dataBroker;

    public void setDepMgr(DependencyManager depMgr) {
        this.depMgr = depMgr;
    }

    public void setDataBroker(DataBrokerService dataBroker) {
        this.dataBroker = dataBroker;
    }


    @Override
    public void mountIcmpDataNode(String nodeId) {
        InstanceIdentifier path = InstanceIdentifier.builder(INVENTORY_PATH)
                .nodeWithKey(INVENTORY_NODE,
                        Collections.<QName, Object>singletonMap(INVENTORY_ID, nodeId )).toInstance();

        MountProvisionInstance mountInstance = depMgr.getMountService().createOrGetMountPoint(path);

        mountInstance.setSchemaContext( depMgr.getGlobalSchemaContext() );

        PingableDeviceHandler handler = new PingableDeviceHandler( nodeId );
        handler.setDataBrokerService(dataBroker);
        handler.setMappingService( depMgr.getMappingService() );
        try {
            RpcRegistration registration = mountInstance.addRpcImplementation(
                    QName.create("http://opendaylight.org/samples/icmp/data",
                                 "2014-05-15",
                                 "sendPingNow"), handler );
            closeables.add( registration );

            Registration<DataReader<InstanceIdentifier, CompositeNode>> registerConfigurationReader
                = mountInstance.registerConfigurationReader(ROOT_PATH, handler);
            closeables.add( registerConfigurationReader );

            closeables.add( mountInstance.registerOperationalReader( ROOT_PATH, handler ) );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        closeables.close();
    }

}
