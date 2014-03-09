/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
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
    public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(SchemaContextListener listener) {
        ListenerRegistration<SchemaContextListener> registration = getDelegate().registerSchemaContextListener(listener);
        addRegistration(registration);
        return registration;
    }
}
