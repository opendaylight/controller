/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingObject;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMActionService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Adapter providing Controller DOMMountPoint implementation based on an MD-SAL DOMMountPoint delegate. Services are
 * looked up in the delegate first. If a lookup is unsuccessful, this class attempts to transparently proxy well-known
 * Controller DOMServices on top of their MD-SAL counterparts available from delegate.
 */
@Deprecated
public class DOMMountPointAdapter extends ForwardingObject implements DOMMountPoint {
    private abstract static class CompatFactory<M extends org.opendaylight.mdsal.dom.api.DOMService,
            C extends DOMService> {
        private final Class<M> mdsalClass;

        CompatFactory(final Class<M> mdsalClass) {
            this.mdsalClass = requireNonNull(mdsalClass);
        }

        final @Nullable C createService(final org.opendaylight.mdsal.dom.api.DOMMountPoint mountPoint) {
            return mountPoint.getService(mdsalClass).map(this::createService).orElse(null);
        }

        abstract C createService(M delegate);
    }

    private static final Map<Class<? extends DOMService>, CompatFactory<?, ?>> KNOWN_SERVICES = ImmutableMap.of(
        DOMActionService.class, new CompatFactory<org.opendaylight.mdsal.dom.api.DOMActionService, DOMActionService>(
                org.opendaylight.mdsal.dom.api.DOMActionService.class) {
            @Override
            DOMActionService createService(final org.opendaylight.mdsal.dom.api.DOMActionService delegate) {
                return new LegacyDOMActionServiceAdapter(delegate);
            }
        },
        DOMDataBroker.class, new CompatFactory<org.opendaylight.mdsal.dom.api.DOMDataBroker, DOMDataBroker>(
                org.opendaylight.mdsal.dom.api.DOMDataBroker.class) {
            @Override
            DOMDataBroker createService(final org.opendaylight.mdsal.dom.api.DOMDataBroker delegate) {
                return new LegacyDOMDataBrokerAdapter(delegate);
            }
        },
        DOMNotificationService.class, new CompatFactory<org.opendaylight.mdsal.dom.api.DOMNotificationService,
                DOMNotificationService>(org.opendaylight.mdsal.dom.api.DOMNotificationService.class) {
            @Override
            DOMNotificationService createService(final org.opendaylight.mdsal.dom.api.DOMNotificationService delegate) {
                return new LegacyDOMNotificationServiceAdapter(delegate);
            }
        },
        DOMRpcService.class, new CompatFactory<org.opendaylight.mdsal.dom.api.DOMRpcService, DOMRpcService>(
                org.opendaylight.mdsal.dom.api.DOMRpcService.class) {
            @Override
            DOMRpcService createService(final org.opendaylight.mdsal.dom.api.DOMRpcService delegate) {
                return new LegacyDOMRpcServiceAdapter(delegate);
            }
        });

    private final org.opendaylight.mdsal.dom.api.DOMMountPoint delegate;

    public DOMMountPointAdapter(final org.opendaylight.mdsal.dom.api.DOMMountPoint delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public YangInstanceIdentifier getIdentifier() {
        return delegate().getIdentifier();
    }

    @Override
    public <T extends DOMService> Optional<T> getService(final Class<T> cls) {
        final java.util.Optional<T> found = delegate.getService(cls);
        if (found.isPresent()) {
            return Optional.of(found.get());
        }

        final CompatFactory<?, ?> compat = KNOWN_SERVICES.get(cls);
        return Optional.fromNullable(cls.cast(compat == null ? null : compat.createService(delegate)));
    }

    @Override
    public SchemaContext getSchemaContext() {
        return delegate().getSchemaContext();
    }

    @Override
    public int hashCode() {
        return getIdentifier().hashCode();
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
        return getIdentifier().equals(other.getIdentifier());
    }

    @Override
    protected org.opendaylight.mdsal.dom.api.DOMMountPoint delegate() {
        return delegate;
    }
}
