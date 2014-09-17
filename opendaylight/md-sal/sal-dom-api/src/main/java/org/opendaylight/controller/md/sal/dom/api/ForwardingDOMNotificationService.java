/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.Collection;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Utility implementation of a {@link DOMNotificationService} which forwards all requests
 * to a delegate instance.
 */
public abstract class ForwardingDOMNotificationService implements DOMNotificationService {
    /**
     * Return the delegate {@link DOMNotificationService} instance.
     *
     * @return Delegate instance.
     */
    protected abstract DOMNotificationService delegate();

    @Override
    public DOMNotificationListenerRegistration registerNotificationListener(final DOMNotificationListener listener,
            final Collection<SchemaPath> types) {
        return delegate().registerNotificationListener(listener, types);
    }

    @Override
    public DOMNotificationListenerRegistration registerNotificationListener(final DOMNotificationListener listener,
            final SchemaPath... types) {
        return delegate().registerNotificationListener(listener, types);
    }
}
