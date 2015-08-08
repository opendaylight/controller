/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.mount;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.md.sal.dom.broker.spi.mount.SimpleDOMMountPoint;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DOMMountPointServiceImpl implements DOMMountPointService {

    private final Map<YangInstanceIdentifier, DOMMountPoint> mountPoints = new HashMap<>();

    private final ListenerRegistry<MountProvisionListener> listeners = ListenerRegistry.create();

    @Override
    public Optional<DOMMountPoint> getMountPoint(final YangInstanceIdentifier path) {
        return Optional.fromNullable(mountPoints.get(path));
    }

    @Override
    public DOMMountPointBuilder createMountPoint(final YangInstanceIdentifier path) {
        Preconditions.checkState(!mountPoints.containsKey(path), "Mount point already exists");
        return new DOMMountPointBuilderImpl(path);
    }

    public void notifyMountCreated(final YangInstanceIdentifier identifier) {
        for (final ListenerRegistration<MountProvisionListener> listener : listeners
                .getListeners()) {
            listener.getInstance().onMountPointCreated(identifier);
        }
    }

    public void notifyMountRemoved(final YangInstanceIdentifier identifier) {
        for (final ListenerRegistration<MountProvisionListener> listener : listeners
                .getListeners()) {
            listener.getInstance().onMountPointRemoved(identifier);
        }
    }

    @Override
    public ListenerRegistration<MountProvisionListener> registerProvisionListener(
            final MountProvisionListener listener) {
        return listeners.register(listener);
    }

    public ObjectRegistration<DOMMountPoint> registerMountPoint(final DOMMountPoint mountPoint) {
        synchronized (mountPoints) {
            Preconditions.checkState(!mountPoints.containsKey(mountPoint.getIdentifier()), "Mount point already exists");
            mountPoints.put(mountPoint.getIdentifier(), mountPoint);
        }
        notifyMountCreated(mountPoint.getIdentifier());

        return new MountRegistration(mountPoint);
    }

    public void unregisterMountPoint(final YangInstanceIdentifier mountPointId) {
        synchronized (mountPoints) {
            Preconditions.checkState(mountPoints.containsKey(mountPointId), "Mount point does not exist");
            mountPoints.remove(mountPointId);
        }
        notifyMountRemoved(mountPointId);
    }

    public class DOMMountPointBuilderImpl implements DOMMountPointBuilder {

        ClassToInstanceMap<DOMService> services = MutableClassToInstanceMap.create();
        private SimpleDOMMountPoint mountPoint;
        private final YangInstanceIdentifier path;
        private SchemaContext schemaContext;

        public DOMMountPointBuilderImpl(final YangInstanceIdentifier path) {
            this.path = path;
        }

        @Override
        public <T extends DOMService> DOMMountPointBuilder addService(final Class<T> type, final T impl) {
            services.putInstance(type, impl);
            return this;
        }

        @Override
        public DOMMountPointBuilder addInitialSchemaContext(final SchemaContext ctx) {
            schemaContext = ctx;
            return this;
        }

        @Override
        public ObjectRegistration<DOMMountPoint> register() {
            Preconditions.checkState(mountPoint == null, "Mount point is already built.");
            mountPoint = SimpleDOMMountPoint.create(path, services,schemaContext);
            return registerMountPoint(mountPoint);
        }
    }

    private final class MountRegistration implements ObjectRegistration<DOMMountPoint> {
        private final DOMMountPoint mountPoint;

        public MountRegistration(final DOMMountPoint mountPoint) {
            this.mountPoint = mountPoint;
        }

        @Override
        public DOMMountPoint getInstance() {
            return mountPoint;
        }

        @Override
        public void close() throws Exception {
            unregisterMountPoint(mountPoint.getIdentifier());
        }
    }
}
