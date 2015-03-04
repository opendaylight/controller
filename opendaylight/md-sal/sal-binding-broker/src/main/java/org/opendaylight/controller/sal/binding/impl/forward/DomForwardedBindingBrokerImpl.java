/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl.forward;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.impl.RootBindingAwareBroker;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingDomConnectorDeployer;
import org.opendaylight.controller.sal.binding.impl.connect.dom.BindingIndependentConnector;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DomForwardedBindingBrokerImpl extends RootBindingAwareBroker implements DomForwardedBroker {

    private ProviderSession domProviderContext;
    private BindingIndependentConnector connector;

    private MountProvisionService domMountService;

    private final DomMountPointForwardingManager domForwardingManager = new DomMountPointForwardingManager();
    private final BindingMountPointForwardingManager bindingForwardingManager = new BindingMountPointForwardingManager();

    private ConcurrentMap<InstanceIdentifier<?>, BindingIndependentConnector> connectors = new ConcurrentHashMap<>();
    private ConcurrentMap<InstanceIdentifier<?>, org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier> forwarded = new ConcurrentHashMap<>();
    private ListenerRegistration<MountProvisionListener> domListenerRegistration;
    private ListenerRegistration<org.opendaylight.controller.sal.binding.api.mount.MountProviderService.MountProvisionListener> baListenerRegistration;


    public DomForwardedBindingBrokerImpl(String instanceName) {
        super(instanceName);
    }

    @Override
    public BindingIndependentConnector getConnector() {
        return connector;
    }

    @Override
    public ProviderSession getDomProviderContext() {
        return domProviderContext;
    }

    @Override
    public void setConnector(BindingIndependentConnector connector) {
        this.connector = connector;
    }

    @Override
    public void setDomProviderContext(ProviderSession domProviderContext) {
        this.domProviderContext = domProviderContext;
    }

    @Override
    public void startForwarding() {
        BindingDomConnectorDeployer.startDataForwarding(getConnector(), getDataBroker(), getDomProviderContext());
        BindingDomConnectorDeployer.startRpcForwarding(getConnector(), getRpcProviderRegistry(),
                getDomProviderContext());
        BindingDomConnectorDeployer.startNotificationForwarding(getConnector(), getNotificationBroker(),
                getDomProviderContext());
        startMountpointForwarding();
    }

    private void startMountpointForwarding() {
        domMountService = getDomProviderContext().getService(MountProvisionService.class);
        if (domMountService != null && getMountManager() != null) {
            domListenerRegistration = domMountService.registerProvisionListener(domForwardingManager);
            baListenerRegistration = getMountManager().registerProvisionListener(bindingForwardingManager);
        }
    }

    public MountProvisionService getDomMountService() {
        return domMountService;
    }

    public void setDomMountService(MountProvisionService domMountService) {
        this.domMountService = domMountService;
    }

    private void tryToDeployConnector(InstanceIdentifier<?> baPath,
            org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier biPath) {
        org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier previous = forwarded.putIfAbsent(baPath, biPath);
        if (previous != null) {
            return;
        }
        MountProviderInstance baMountPoint = getMountManager().createOrGetMountPoint(baPath);
        MountProvisionInstance domMountPoint = domMountService.createOrGetMountPoint(biPath);
        BindingIndependentConnector connector = createForwarder(baPath, baMountPoint, domMountPoint);
        connectors.put(baPath, connector);
    }

    private BindingIndependentConnector createForwarder(InstanceIdentifier<?> path, MountProviderInstance baMountPoint,
            MountProvisionInstance domMountPoint) {
        BindingIndependentConnector mountConnector = BindingDomConnectorDeployer.createConnector(getConnector());

        BindingDomConnectorDeployer.startDataForwarding(mountConnector, baMountPoint, domMountPoint);
        BindingDomConnectorDeployer.startRpcForwarding(mountConnector, baMountPoint, domMountPoint);
        BindingDomConnectorDeployer.startNotificationForwarding(mountConnector, baMountPoint, domMountPoint);
        // connector.setDomNotificationBroker(domMountPoint);
        return mountConnector;
    }

    public synchronized void tryToDeployDomForwarder(org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier domPath) {
        InstanceIdentifier<?> baPath;
        try {
            baPath = connector.getMappingService().fromDataDom(domPath);
            BindingIndependentConnector potentialConnector = connectors.get(baPath);
            if (potentialConnector != null) {
                return;
            }
            tryToDeployConnector(baPath, domPath);
        } catch (DeserializationException e) {

        }
    }

    public synchronized void tryToDeployBindingForwarder(InstanceIdentifier<?> baPath) {
        BindingIndependentConnector potentialConnector = connectors.get(baPath);
        if (potentialConnector != null) {
            return;
        }
        org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier domPath = connector.getMappingService().toDataDom(baPath);
        tryToDeployConnector(baPath, domPath);
    }

    public synchronized void undeployBindingForwarder(InstanceIdentifier<?> baPath) {
        // FIXME: Implement closeMountPoint
    }

    public synchronized void undeployDomForwarder(org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier biPath) {
        // FIXME: Implement closeMountPoint
    }

    private class DomMountPointForwardingManager implements MountProvisionListener {

        @Override
        public void onMountPointCreated(org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier path) {
//            tryToDeployDomForwarder(path);
        }

        @Override
        public void onMountPointRemoved(org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier path) {
//            undeployDomForwarder(path);
        }
    }

    private class BindingMountPointForwardingManager implements
            org.opendaylight.controller.sal.binding.api.mount.MountProviderService.MountProvisionListener {

        @Override
        public void onMountPointCreated(InstanceIdentifier<?> path) {
            tryToDeployBindingForwarder(path);
        }

        @Override
        public void onMountPointRemoved(InstanceIdentifier<?> path) {
            undeployBindingForwarder(path);
        }
    }

}
