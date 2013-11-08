package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.osgi.framework.ServiceReference;

public class SchemaServiceProxy extends AbstractBrokerServiceProxy<SchemaService> implements SchemaService {

    public SchemaServiceProxy(ServiceReference<SchemaService> ref, SchemaService delegate) {
        super(ref, delegate);
    }

    @Override
    public void addModule(Module module) {
        getDelegate().addModule(module);
    }

    @Override
    public void removeModule(Module module) {
        getDelegate().removeModule(module);
    }

    @Override
    public SchemaContext getSessionContext() {
        return null;
    }

    @Override
    public SchemaContext getGlobalContext() {
        return getDelegate().getGlobalContext();
    }

    @Override
    public ListenerRegistration<SchemaServiceListener> registerSchemaServiceListener(SchemaServiceListener listener) {
        ListenerRegistration<SchemaServiceListener> registration = getDelegate().registerSchemaServiceListener(listener);
        addRegistration(registration);
        return registration;
    }

    
    
}
