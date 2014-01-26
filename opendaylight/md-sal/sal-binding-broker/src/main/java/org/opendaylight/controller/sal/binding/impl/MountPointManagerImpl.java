/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.md.sal.binding.util.AbstractBindingSalProviderInstance;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListeningExecutorService;

public class MountPointManagerImpl implements MountProviderService {

    public final Logger LOG = LoggerFactory.getLogger(MountPointManagerImpl.class);

    private final ConcurrentMap<InstanceIdentifier<?>, BindingMountPointImpl> mountPoints;
    private final ListenerRegistry<MountProvisionListener> listeners = ListenerRegistry.create();
    
    private ListeningExecutorService notificationExecutor;
    private ListeningExecutorService dataCommitExecutor;

    public MountPointManagerImpl() {
        mountPoints = new ConcurrentHashMap<>();
    }

    public ListeningExecutorService getNotificationExecutor() {
        return notificationExecutor;
    }

    public void setNotificationExecutor(ListeningExecutorService notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    public ListeningExecutorService getDataCommitExecutor() {
        return dataCommitExecutor;
    }

    public void setDataCommitExecutor(ListeningExecutorService dataCommitExecutor) {
        this.dataCommitExecutor = dataCommitExecutor;
    }

    @Override
    public synchronized BindingMountPointImpl createMountPoint(InstanceIdentifier<?> path) {
        BindingMountPointImpl potential = mountPoints.get(path);
        if (potential != null) {
            throw new IllegalStateException("Mount point already exists.");
        }
        return createOrGetMountPointImpl(path);
    }

    @Override
    public BindingMountPointImpl createOrGetMountPoint(InstanceIdentifier<?> path) {
        BindingMountPointImpl potential = getMountPoint(path);
        if (potential != null) {
            return potential;
        }
        return createOrGetMountPointImpl(path);
    }

    @Override
    public BindingMountPointImpl getMountPoint(InstanceIdentifier<?> path) {
        return mountPoints.get(path);
    }

    private synchronized BindingMountPointImpl createOrGetMountPointImpl(InstanceIdentifier<?> path) {
        BindingMountPointImpl potential = getMountPoint(path);
        if (potential != null) {
            return potential;
        }
        RpcProviderRegistryImpl rpcRegistry = new RpcProviderRegistryImpl("mount");
        NotificationBrokerImpl notificationBroker = new NotificationBrokerImpl();
        notificationBroker.setExecutor(getNotificationExecutor());
        DataBrokerImpl dataBroker = new DataBrokerImpl();
        dataBroker.setExecutor(getDataCommitExecutor());
        BindingMountPointImpl mountInstance = new BindingMountPointImpl(path, rpcRegistry, notificationBroker,
                dataBroker);
        mountPoints.putIfAbsent(path, mountInstance);
        notifyMountPointCreated(path);
        return mountInstance;
    }

    private void notifyMountPointCreated(InstanceIdentifier<?> path) {
        for (ListenerRegistration<MountProvisionListener> listener : listeners) {
            try {
                listener.getInstance().onMountPointCreated(path);
            } catch (Exception e) {
                LOG.error("Unhandled exception during invoking listener.", e);
            }
        }
    }

    @Override
    public ListenerRegistration<MountProvisionListener> registerProvisionListener(MountProvisionListener listener) {
        return listeners.register(listener);
    }

    public class BindingMountPointImpl extends
            AbstractBindingSalProviderInstance<DataBrokerImpl, NotificationBrokerImpl, RpcProviderRegistryImpl>
    implements MountProviderInstance {

        private InstanceIdentifier<?> identifier;

        public BindingMountPointImpl(org.opendaylight.yangtools.yang.binding.InstanceIdentifier<?> identifier,
                RpcProviderRegistryImpl rpcRegistry, NotificationBrokerImpl notificationBroker,
                DataBrokerImpl dataBroker) {
            super(rpcRegistry, notificationBroker, dataBroker);
            this.identifier = identifier;
        }
        
        @Override
        public InstanceIdentifier<?> getIdentifier() {
            return this.identifier;
        }
    }
}
