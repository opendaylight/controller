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
import java.util.Arrays;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMNotificationListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Deprecated
public class DOMNotificationServiceAdapter extends ForwardingObject
        implements org.opendaylight.mdsal.dom.api.DOMNotificationService {

    private final DOMNotificationService delegate;

    public DOMNotificationServiceAdapter(final DOMNotificationService delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener,
            final Collection<SchemaPath> types) {
        // Controller events are sub-interfaces of MD-SAL events, hence direct routing is okay
        final ListenerRegistration<?> reg = delegate().registerNotificationListener(listener::onNotification, types);

        return new AbstractListenerRegistration<T>(listener) {
            @Override
            protected void removeRegistration() {
                reg.close();

            }
        };
    }

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener,
            final SchemaPath... types) {
        return registerNotificationListener(listener, Arrays.asList(types));
    }

    @Override
    protected DOMNotificationService delegate() {
        return delegate;
    }
}
