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

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sample.pingdiscovery.DeviceMountHandler;
import org.opendaylight.controller.sample.pingdiscovery.dependencies.mgrs.BundleContextDependencyManager;
import org.opendaylight.controller.sample.pingdiscovery.dependencies.mgrs.MountingServiceDependencyManager;
import org.opendaylight.controller.sample.pingdiscovery.util.AutoCloseableManager;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

/**
 * Mounts a specific implementation of an RPC and a DataReader onto the given node. <br>
 * <br>
 * DEMONSTRATES: How to mount RPC and DataReaders to a single node
 * 
 * @author Devin Avery
 * @author Greg Hall
 * 
 */
public class DeviceMountHandlerImpl implements DeviceMountHandler, AutoCloseable {

    private final InstanceIdentifier ROOT_PATH = InstanceIdentifier.builder().build();
    private final AutoCloseableManager closeables = new AutoCloseableManager();
    private MountingServiceDependencyManager depMgr;
    private DataBrokerService dataBroker;
    private BundleContextDependencyManager bundleContextMgr;

    public void setDependencyMgr(MountingServiceDependencyManager depMgr) {
        this.depMgr = depMgr;
    }

    public void setDataBrokerService(DataBrokerService dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void mountIcmpDataNode(String nodeId) {

        // Construct a new node for this particular node id.
        InstanceIdentifier path = createPath(nodeId);
        MountProvisionInstance mountInstance = depMgr.getMountService().createOrGetMountPoint(path);

        // Describes what schemas are available from this mount point.
        // TODO: Should we really only be returning our one supported yang file?
        mountInstance.setSchemaContext(depMgr.getGlobalSchemaContext());

        // A new implementation of a handler for EACH node means different
        // functionality registered for the same RPC on different nodes.
        PingableDeviceHandler handler = new PingableDeviceHandler(nodeId);
        handler.setDataBrokerService(dataBroker);
        handler.setMappingService(bundleContextMgr.getMappingService());

        handler.setPingService(new PingServiceExecSystemImpl());

        try {
            // Register the handler for RPC calls for this node/RPC. The context of node is
            // maintained by the mountInstance mount point.
            RpcRegistration registration = mountInstance.addRpcImplementation(QName.create(
                    "http://opendaylight.org/samples/icmp/data", "2014-05-15", "sendPingNow"),
                    handler);
            closeables.add(registration);

            // Registers this as the handler for yang data model reads to everything under the mount
            // point. You could use a different object here than the RPC handler if you wanted. WE
            // used the same one for simplicity.
            // NOTE: we opted for one handler on both config & operational stores.
            closeables.add(mountInstance.registerConfigurationReader(ROOT_PATH, handler));
            closeables.add(mountInstance.registerOperationalReader(ROOT_PATH, handler));
        } catch (Exception e) {
            // Something bad happened ... not sure what, pass this to the poor end user to figure
            // out until the poor error model improves.
            e.printStackTrace();
        }
    }

    /**
     * @param nodeId
     * @return
     */
    private InstanceIdentifier createPath(String nodeId) {
        InstanceIdentifier path = InstanceIdentifier
                .builder(INVENTORY_PATH)
                .nodeWithKey(INVENTORY_NODE,
                        Collections.<QName, Object> singletonMap(INVENTORY_ID, nodeId))
                        .toInstance();
        return path;
    }

    @Override
    public void close() throws Exception {
        closeables.close();
    }

    public void setBundleContextDepMgr(BundleContextDependencyManager bundleContextMgr) {
        this.bundleContextMgr = bundleContextMgr;
    }

}
