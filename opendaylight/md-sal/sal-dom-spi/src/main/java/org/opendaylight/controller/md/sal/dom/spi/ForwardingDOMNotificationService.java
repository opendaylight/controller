/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.collect.ForwardingObject;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Utility implementation of a {@link DOMNotificationService} which forwards all requests
 * to a delegate instance.
 */
public abstract class ForwardingDOMNotificationService extends ForwardingObject implements DOMNotificationService {
    @Override
    protected abstract DOMNotificationService delegate();

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener,
            final Collection<SchemaPath> types) {
        return delegate().registerNotificationListener(listener, types);
    }

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener,
            final SchemaPath... types) {
        return delegate().registerNotificationListener(listener, types);
    }
}
