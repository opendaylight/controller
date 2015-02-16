/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class CurrentSchemaContext implements SchemaContextListener, AutoCloseable {
    final AtomicReference<SchemaContext> currentContext = new AtomicReference<SchemaContext>();
    private final ListenerRegistration<SchemaContextListener> schemaContextListenerListenerRegistration;

    public SchemaContext getCurrentContext() {
        Preconditions.checkState(currentContext.get() != null, "Current context not received");
        return currentContext.get();
    }

    public CurrentSchemaContext(final SchemaService schemaService) {
        schemaContextListenerListenerRegistration = schemaService.registerSchemaContextListener(this);
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext schemaContext) {
        currentContext.set(schemaContext);
    }

    @Override
    public void close() throws Exception {
        schemaContextListenerListenerRegistration.close();
        currentContext.set(null);
    }
}