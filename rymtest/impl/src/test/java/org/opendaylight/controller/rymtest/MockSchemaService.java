package org.opendaylight.controller.rymtest;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaServiceExtension;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;

public final class MockSchemaService implements DOMSchemaService, SchemaContextProvider {

    private SchemaContext schemaContext;

    ListenerRegistry<SchemaContextListener> listeners = ListenerRegistry.create();

    @Override
    public synchronized SchemaContext getGlobalContext() {
        return schemaContext;
    }

    @Override
    public synchronized SchemaContext getSessionContext() {
        return schemaContext;
    }

    @Override
    public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(
            final SchemaContextListener listener) {
        return listeners.register(listener);
    }

    @Override
    public synchronized SchemaContext getSchemaContext() {
        return schemaContext;
    }

    @Override
    public ClassToInstanceMap<DOMSchemaServiceExtension> getExtensions() {
        return ImmutableClassToInstanceMap.of();
    }

    public synchronized void changeSchema(final SchemaContext newContext) {
        schemaContext = newContext;
        for (ListenerRegistration<? extends SchemaContextListener> listener : listeners.getRegistrations()) {
            listener.getInstance().onGlobalContextUpdated(schemaContext);
        }
    }
}
