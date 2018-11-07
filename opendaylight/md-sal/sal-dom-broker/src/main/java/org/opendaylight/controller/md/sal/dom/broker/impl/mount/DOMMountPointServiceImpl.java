/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl.mount;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MutableClassToInstanceMap;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.dom.api.DOMActionService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.md.sal.dom.broker.spi.mount.SimpleDOMMountPoint;
import org.opendaylight.controller.sal.core.compat.DOMDataBrokerAdapter;
import org.opendaylight.controller.sal.core.compat.DOMMountPointAdapter;
import org.opendaylight.controller.sal.core.compat.DOMNotificationServiceAdapter;
import org.opendaylight.controller.sal.core.compat.DOMRpcServiceAdapter;
import org.opendaylight.mdsal.dom.api.DOMMountPointListener;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class DOMMountPointServiceImpl implements DOMMountPointService {
    private abstract static class CompatFactory<M extends org.opendaylight.mdsal.dom.api.DOMService,
            C extends DOMService> {
        private final Class<C> controllerClass;
        private final Class<M> mdsalClass;

        CompatFactory(final Class<C> controllerClass, final Class<M> mdsalClass) {
            this.controllerClass = requireNonNull(controllerClass);
            this.mdsalClass = requireNonNull(mdsalClass);
        }

        final void addService(final org.opendaylight.mdsal.dom.api.DOMMountPointService.DOMMountPointBuilder builder,
                final ClassToInstanceMap<DOMService> services) {
            if (!services.containsKey(mdsalClass)) {
                final C controllerService = services.getInstance(controllerClass);
                if (controllerService != null) {
                    final M mdsalService = createService(controllerService);
                    if (mdsalService != null) {
                        builder.addService(mdsalClass, mdsalService);
                    }
                }
            }
        }

        abstract M createService(C delegate);
    }

    private static final Map<Class<? extends DOMService>, CompatFactory<?, ?>> KNOWN_SERVICES = ImmutableMap.of(
        DOMActionService.class, new CompatFactory<org.opendaylight.mdsal.dom.api.DOMActionService, DOMActionService>(
                DOMActionService.class, org.opendaylight.mdsal.dom.api.DOMActionService.class) {
            @Override
            org.opendaylight.mdsal.dom.api.DOMActionService createService(final DOMActionService delegate) {
                return delegate;
            }
        },
        DOMDataBroker.class, new CompatFactory<org.opendaylight.mdsal.dom.api.DOMDataBroker, DOMDataBroker>(
                DOMDataBroker.class, org.opendaylight.mdsal.dom.api.DOMDataBroker.class) {
            @Override
            org.opendaylight.mdsal.dom.api.DOMDataBroker createService(final DOMDataBroker delegate) {
                return new DOMDataBrokerAdapter(delegate);
            }
        },
        DOMNotificationService.class, new CompatFactory<org.opendaylight.mdsal.dom.api.DOMNotificationService,
                DOMNotificationService>(DOMNotificationService.class,
                        org.opendaylight.mdsal.dom.api.DOMNotificationService.class) {
            @Override
            org.opendaylight.mdsal.dom.api.DOMNotificationService createService(final DOMNotificationService delegate) {
                return new DOMNotificationServiceAdapter(delegate);
            }
        },
        DOMRpcService.class, new CompatFactory<org.opendaylight.mdsal.dom.api.DOMRpcService, DOMRpcService>(
                DOMRpcService.class, org.opendaylight.mdsal.dom.api.DOMRpcService.class) {
            @Override
            org.opendaylight.mdsal.dom.api.DOMRpcService createService(final DOMRpcService delegate) {
                return new DOMRpcServiceAdapter(delegate);
            }
        });

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
        return Optional.fromJavaUtil(delegate.getMountPoint(path).map(DOMMountPointAdapter::new));
    }

    @Override
    public DOMMountPointBuilder createMountPoint(final YangInstanceIdentifier path) {
        return new DOMMountPointBuilderImpl(path);
    }

    @Override
    public ListenerRegistration<DOMMountPointListener> registerProvisionListener(final DOMMountPointListener listener) {
        return delegate.registerProvisionListener(listener);
    }

    @SuppressWarnings("unchecked")
    ObjectRegistration<DOMMountPoint> registerMountPoint(final SimpleDOMMountPoint mountPoint) {
        final org.opendaylight.mdsal.dom.api.DOMMountPointService.DOMMountPointBuilder delegateBuilder =
                delegate.createMountPoint(mountPoint.getIdentifier());

        if (mountPoint.getSchemaContext() != null) {
            delegateBuilder.addInitialSchemaContext(mountPoint.getSchemaContext());
        }

        final ClassToInstanceMap<DOMService> myServices = mountPoint.getServices();
        for (Entry<Class<? extends DOMService>, DOMService> entry : myServices.entrySet()) {
            delegateBuilder.addService((Class<DOMService>)entry.getKey(), entry.getValue());

            final CompatFactory<?, ?> compat = KNOWN_SERVICES.get(entry.getKey());
            if (compat != null) {
                compat.addService(delegateBuilder, myServices);
            }
        }

        final ObjectRegistration<org.opendaylight.mdsal.dom.api.DOMMountPoint> delegateReg = delegateBuilder.register();
        return new AbstractObjectRegistration<DOMMountPoint>(mountPoint) {
            @Override
            protected void removeRegistration() {
                delegateReg.close();
            }
        };
    }

    public class DOMMountPointBuilderImpl implements DOMMountPointBuilder {
        private final ClassToInstanceMap<DOMService> services = MutableClassToInstanceMap.create();
        private final YangInstanceIdentifier path;
        private SimpleDOMMountPoint mountPoint;
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
