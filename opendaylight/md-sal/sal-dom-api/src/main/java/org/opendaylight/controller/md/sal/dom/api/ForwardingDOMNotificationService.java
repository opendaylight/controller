/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Utility implementation of a {@link DOMNotificationService} which forwards all requests
 * to a delegate instance.
 */
public final class ForwardingDOMNotificationService implements DOMNotificationService {
    private final DOMNotificationService delegate;

    public ForwardingDOMNotificationService(final DOMNotificationService delegate) {
        this.delegate = Preconditions.checkNotNull(delegate);
    }

    @Override
    public DOMNotificationListenerRegistration registerNotificationListener(final DOMNotificationListener listener,
            final Collection<SchemaPath> types) {
        return delegate.registerNotificationListener(listener, types);
    }

    @Override
    public DOMNotificationListenerRegistration registerNotificationListener(final DOMNotificationListener listener,
            final SchemaPath... types) {
        return delegate.registerNotificationListener(listener, types);
    }
}
