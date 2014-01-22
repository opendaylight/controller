package org.opendaylight.controller.sal.binding.impl.connect.dom;

import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BindingIndependentMountPointForwarder {

    private MountProvisionService domMountService;
    private MountProviderService baMountService;
    private BindingIndependentMappingService mappingService;

    private final DomMountPointForwardingManager domForwardingManager = new DomMountPointForwardingManager();
    private final BindingMountPointForwardingManager bindingForwardingManager = new BindingMountPointForwardingManager();

    private ConcurrentMap<InstanceIdentifier<?>, BindingIndependentConnector> connectors;
    private ConcurrentMap<InstanceIdentifier<?>, org.opendaylight.yangtools.yang.data.api.InstanceIdentifier> forwarded;
    private ListenerRegistration<MountProvisionListener> domListenerRegistration;
    private ListenerRegistration<org.opendaylight.controller.sal.binding.api.mount.MountProviderService.MountProvisionListener> baListenerRegistration;

    public MountProvisionService getDomMountService() {
        return domMountService;
    }

    public void setDomMountService(MountProvisionService domMountService) {
        this.domMountService = domMountService;
    }

    public void start() {
        if(domMountService != null && baMountService != null) {
            domListenerRegistration = domMountService.registerProvisionListener(domForwardingManager);
            baListenerRegistration = baMountService.registerProvisionListener(bindingForwardingManager);
        }
    }

    private void tryToDeployConnector(InstanceIdentifier<?> baPath,
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biPath) {
        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier previous = forwarded.putIfAbsent(baPath, biPath);
        if(previous != null) {
            return;
        }
        MountProviderInstance baMountPoint = baMountService.getMountPoint(baPath);
        MountProvisionInstance domMountPoint = domMountService.getMountPoint(biPath);
        BindingIndependentConnector connector = createForwarder(baPath, baMountPoint, domMountPoint);
        connectors.put(baPath, connector);
        connector.startDataForwarding();
        connector.startRpcForwarding();
        connector.startNotificationForwarding();
    }

    private BindingIndependentConnector createForwarder(InstanceIdentifier<?> path, MountProviderInstance baMountPoint,
            MountProvisionInstance domMountPoint) {
        BindingIndependentConnector connector = new BindingIndependentConnector();

        connector.setBindingDataService(baMountPoint);
        connector.setBindingRpcRegistry(baMountPoint);
        //connector.setBindingNotificationBroker(baMountPoint);

        connector.setDomDataService(domMountPoint);
        connector.setDomRpcRegistry(domMountPoint);
        //connector.setDomNotificationBroker(domMountPoint);
        return connector;
    }

    public synchronized void tryToDeployDomForwarder(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier domPath) {
        InstanceIdentifier<?> baPath;
        try {
            baPath = mappingService.fromDataDom(domPath);
            BindingIndependentConnector potentialConnector = connectors.get(baPath);
            if(potentialConnector != null) {
                return;
            }
            tryToDeployConnector(baPath,domPath);
        } catch (DeserializationException e) {

        }
    }

    public synchronized void tryToDeployBindingForwarder(InstanceIdentifier<?> baPath) {
        BindingIndependentConnector potentialConnector =connectors.get(baPath);
        if(potentialConnector != null) {
            return;
        }
        org.opendaylight.yangtools.yang.data.api.InstanceIdentifier domPath = mappingService.toDataDom(baPath);
        tryToDeployConnector(baPath, domPath);
    }

    public synchronized void undeployBindingForwarder(InstanceIdentifier<?> baPath) {
        // FIXME: Implement closeMountPoint
    }

    public synchronized void undeployDomForwarder(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier biPath) {
        // FIXME: Implement closeMountPoint
    }

    private class DomMountPointForwardingManager implements MountProvisionListener {

        @Override
        public void onMountPointCreated(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier path) {
            tryToDeployDomForwarder(path);
        }

        @Override
        public void onMountPointRemoved(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier path) {
            undeployDomForwarder(path);
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
