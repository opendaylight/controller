/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.mount;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.md.sal.dom.broker.spi.mount.SimpleDOMMountPoint;
import org.opendaylight.mdsal.dom.api.DOMMountPointListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DOMMountPointServiceImpl implements DOMMountPointService {

    private final org.opendaylight.mdsal.dom.api.DOMMountPointService delegate;

    @VisibleForTesting
    public DOMMountPointServiceImpl() {
        this(new org.opendaylight.mdsal.dom.broker.DOMMountPointServiceImpl());
    }

    public DOMMountPointServiceImpl(final org.opendaylight.mdsal.dom.api.DOMMountPointService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<DOMMountPoint> getMountPoint(final YangInstanceIdentifier path) {
        return Optional.fromJavaUtil(delegate.getMountPoint(path).map(DOMMountPointServiceImpl::convert));
    }

    private static DOMMountPoint convert(final org.opendaylight.mdsal.dom.api.DOMMountPoint from) {
        return new DOMMountPoint() {
            @Override
            public YangInstanceIdentifier getIdentifier() {
                return from.getIdentifier();
            }

            @Override
            public <T extends DOMService> Optional<T> getService(final Class<T> cls) {
                return Optional.fromJavaUtil(from.getService(cls));
            }

            @Override
            public SchemaContext getSchemaContext() {
                return from.getSchemaContext();
            }

            @Override
            public int hashCode() {
                return from.getIdentifier().hashCode();
            }

            @Override
            public boolean equals(final Object obj) {
                if (this == obj) {
                    return true;
                }

                if (!(obj instanceof DOMMountPoint)) {
                    return false;
                }

                DOMMountPoint other = (DOMMountPoint) obj;
                return from.getIdentifier().equals(other.getIdentifier());
            }
        };
    }

    @Override
    public DOMMountPointBuilder createMountPoint(final YangInstanceIdentifier path) {
        return new DOMMountPointBuilderImpl(path);
    }

    @Override
    public ListenerRegistration<DOMMountPointListener> registerProvisionListener(final DOMMountPointListener listener) {
        return delegate.registerProvisionListener(listener);
    }

    /**
     * Deprecated.

     * @deprecated this method should never have been exposed publicly - registration should be done via the
     *         public {@link #createMountPoint} interface. As such, this method expects the {@code mountPoint} param
     *         to be of type {@link SimpleDOMMountPoint}.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public ObjectRegistration<DOMMountPoint> registerMountPoint(final DOMMountPoint mountPoint) {
        Preconditions.checkArgument(mountPoint instanceof SimpleDOMMountPoint, "Expected SimpleDOMMountPoint");

        final org.opendaylight.mdsal.dom.api.DOMMountPointService.DOMMountPointBuilder delegateBuilder =
            delegate.createMountPoint(mountPoint.getIdentifier());

        if (mountPoint.getSchemaContext() != null) {
            delegateBuilder.addInitialSchemaContext(mountPoint.getSchemaContext());
        }

        ((SimpleDOMMountPoint)mountPoint).getAllServices().forEach(
            entry -> delegateBuilder.addService((Class<DOMService>)entry.getKey(), entry.getValue()));

        final ObjectRegistration<org.opendaylight.mdsal.dom.api.DOMMountPoint> delegateReg = delegateBuilder.register();
        return new ObjectRegistration<DOMMountPoint>() {
            @Override
            public void close() {
                delegateReg.close();
            }

            @Override
            public DOMMountPoint getInstance() {
                return mountPoint;
            }
        };
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
            mountPoint = SimpleDOMMountPoint.create(path, services, schemaContext);
            return registerMountPoint(mountPoint);
        }
    }
}
