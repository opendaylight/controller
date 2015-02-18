/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.opendaylight.controller.netconf.api.Capability;
import org.opendaylight.controller.netconf.api.monitoring.CapabilityListener;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class CurrentSchemaContext implements SchemaContextListener, AutoCloseable {
    final AtomicReference<SchemaContext> currentContext = new AtomicReference<SchemaContext>();
    private final ListenerRegistration<SchemaContextListener> schemaContextListenerListenerRegistration;
    private final Set<CapabilityListener> listeners = Collections.synchronizedSet(Sets.<CapabilityListener>newHashSet());

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
        // FIXME is notifying all the listeners from this callback wise ?
        final Set<Capability> addedCaps = MdsalNetconfOperationServiceFactory.transformCapabilities(currentContext.get());
        for (final CapabilityListener listener : listeners) {
            listener.onCapabilitiesAdded(addedCaps);
        }
    }

    @Override
    public void close() throws Exception {
        listeners.clear();
        schemaContextListenerListenerRegistration.close();
        currentContext.set(null);
    }

    public AutoCloseable registerCapabilityListener(final CapabilityListener listener) {
        listener.onCapabilitiesAdded(MdsalNetconfOperationServiceFactory.transformCapabilities(currentContext.get()));
        listeners.add(listener);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                listeners.remove(listener);
            }
        };
    }
}