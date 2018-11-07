/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.spi.mount;

import com.google.common.base.Optional;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public final class SimpleDOMMountPoint implements DOMMountPoint {

    private final YangInstanceIdentifier identifier;
    private final ClassToInstanceMap<DOMService> services;
    private final SchemaContext schemaContext;

    public static SimpleDOMMountPoint create(final YangInstanceIdentifier identifier,
            final ClassToInstanceMap<DOMService> services, final SchemaContext ctx) {
        return new SimpleDOMMountPoint(identifier, services, ctx);
    }

    private SimpleDOMMountPoint(final YangInstanceIdentifier identifier,
            final ClassToInstanceMap<DOMService> services, final SchemaContext ctx) {
        this.identifier = identifier;
        this.services = ImmutableClassToInstanceMap.copyOf(services);
        this.schemaContext = ctx;
    }

    @Override
    public YangInstanceIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    @Override
    public <T extends DOMService> Optional<T> getService(final Class<T> cls) {
        return Optional.fromNullable(services.getInstance(cls));
    }

    public Set<Entry<Class<? extends DOMService>, DOMService>> getAllServices() {
        return services.entrySet();
    }

    public ClassToInstanceMap<DOMService> getServices() {
        return services;
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof DOMMountPoint && identifier.equals(((DOMMountPoint) obj).getIdentifier());
    }
}
