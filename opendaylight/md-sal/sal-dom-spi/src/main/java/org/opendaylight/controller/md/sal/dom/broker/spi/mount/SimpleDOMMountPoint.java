package org.opendaylight.controller.md.sal.dom.broker.spi.mount;

import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;

public class SimpleDOMMountPoint implements DOMMountPoint {

    private final YangInstanceIdentifier identifier;
    private final ClassToInstanceMap<DOMService> services;
    private final SchemaContext schemaContext;

    public static final SimpleDOMMountPoint create(final YangInstanceIdentifier identifier, final ClassToInstanceMap<DOMService> services, final SchemaContext ctx) {
        return new SimpleDOMMountPoint(identifier, services, ctx);
    }
    private SimpleDOMMountPoint(final YangInstanceIdentifier identifier, final ClassToInstanceMap<DOMService> services, final SchemaContext ctx) {
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
}
