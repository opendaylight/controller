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
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.binding.util.AbstractBindingSalProviderInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 *
 *
 * @deprecated This class implements legacy mount point APIs and should
 *   be replaced with newer one.
 */
@Deprecated
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

    public void setNotificationExecutor(final ListeningExecutorService notificationExecutor) {
        this.notificationExecutor = notificationExecutor;
    }

    public ListeningExecutorService getDataCommitExecutor() {
        return dataCommitExecutor;
    }

    public void setDataCommitExecutor(final ListeningExecutorService dataCommitExecutor) {
        this.dataCommitExecutor = dataCommitExecutor;
    }

    @Override
    public synchronized BindingMountPointImpl createMountPoint(final InstanceIdentifier<?> path) {
        final BindingMountPointImpl potential = mountPoints.get(path);
        if (potential != null) {
            throw new IllegalStateException("Mount point already exists.");
        }
        return createOrGetMountPointImpl(path);
    }

    @Override
    public BindingMountPointImpl createOrGetMountPoint(final InstanceIdentifier<?> path) {
        final BindingMountPointImpl potential = getMountPoint(path);
        if (potential != null) {
            return potential;
        }
        return createOrGetMountPointImpl(path);
    }

    @Override
    public BindingMountPointImpl getMountPoint(final InstanceIdentifier<?> path) {
        return mountPoints.get(path);
    }

    private synchronized BindingMountPointImpl createOrGetMountPointImpl(final InstanceIdentifier<?> path) {
        final BindingMountPointImpl potential = getMountPoint(path);
        if (potential != null) {
            return potential;
        }
        final RpcProviderRegistryImpl rpcRegistry = new RpcProviderRegistryImpl("mount");
        final NotificationBrokerImpl notificationBroker = new NotificationBrokerImpl(getNotificationExecutor());
        final DataBrokerImpl dataBroker = new DataBrokerImpl();
        dataBroker.setExecutor(MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
        final BindingMountPointImpl mountInstance = new BindingMountPointImpl(path, rpcRegistry, notificationBroker,
                dataBroker);
        mountPoints.putIfAbsent(path, mountInstance);
        notifyMountPointCreated(path);
        return mountInstance;
    }

    private void notifyMountPointCreated(final InstanceIdentifier<?> path) {
        for (final ListenerRegistration<MountProvisionListener> listener : listeners) {
            try {
                listener.getInstance().onMountPointCreated(path);
            } catch (final Exception e) {
                LOG.error("Unhandled exception during invoking listener.", e);
            }
        }
    }

    @Override
    public ListenerRegistration<MountProvisionListener> registerProvisionListener(final MountProvisionListener listener) {
        return listeners.register(listener);
    }

    public class BindingMountPointImpl extends
    AbstractBindingSalProviderInstance<DataBrokerImpl, NotificationBrokerImpl, RpcProviderRegistryImpl>
    implements MountProviderInstance {

        private final InstanceIdentifier<?> identifier;

        public BindingMountPointImpl(final InstanceIdentifier<?> identifier,
                final RpcProviderRegistryImpl rpcRegistry, final NotificationBrokerImpl notificationBroker,
                final DataBrokerImpl dataBroker) {
            super(rpcRegistry, notificationBroker, dataBroker);
            this.identifier = identifier;
        }

        // Needed only for BI Connector
        public DataBrokerImpl getDataBrokerImpl() {
            return getDataBroker();
        }

        @Override
        public InstanceIdentifier<?> getIdentifier() {
            return this.identifier;
        }
    }
}
