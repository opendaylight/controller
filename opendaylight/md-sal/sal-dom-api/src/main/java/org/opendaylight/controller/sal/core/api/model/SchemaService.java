/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.model;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

/**
 * @deprecated Use {@link DOMSchemaService} instead.
 */
@Deprecated
public interface SchemaService extends BrokerService, DOMSchemaService {

    /**
     * Registers a YANG module to session and global context
     */
    void addModule(Module module);

    /**
     * Unregisters a YANG module from session context
     */
    void removeModule(Module module);

    /**
     * Returns session specific YANG schema context
     */
    @Override
    SchemaContext getSessionContext();

    /**
     * Returns global schema context
     */
    @Override
    SchemaContext getGlobalContext();

    /**
     * Register a listener for changes in schema context.
     *
     * @param listener Listener which should be registered
     * @return Listener registration handle
     */
    @Override
    ListenerRegistration<SchemaContextListener> registerSchemaContextListener(SchemaContextListener listener);
}
