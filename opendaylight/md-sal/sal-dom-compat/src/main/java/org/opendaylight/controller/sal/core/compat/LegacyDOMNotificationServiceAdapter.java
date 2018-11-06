/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ForwardingObject;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Deprecated
public class LegacyDOMNotificationServiceAdapter extends ForwardingObject implements DOMNotificationService {
    private final org.opendaylight.mdsal.dom.api.DOMNotificationService delegate;

    public LegacyDOMNotificationServiceAdapter(final org.opendaylight.mdsal.dom.api.DOMNotificationService delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener,
            final Collection<SchemaPath> types) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener,
            final SchemaPath... types) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected org.opendaylight.mdsal.dom.api.DOMNotificationService delegate() {
        return delegate;
    }
}
