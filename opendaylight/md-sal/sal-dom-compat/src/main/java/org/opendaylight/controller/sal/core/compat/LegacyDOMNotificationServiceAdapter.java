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
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import org.opendaylight.controller.md.sal.dom.api.DOMEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
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
        final ListenerRegistration<org.opendaylight.mdsal.dom.api.DOMNotificationListener> reg =
                delegate().registerNotificationListener(notification -> {
                    if (notification instanceof DOMNotification) {
                        listener.onNotification((DOMNotification)notification);
                        return;
                    }

                    if (notification instanceof org.opendaylight.mdsal.dom.api.DOMEvent) {
                        listener.onNotification(new DefaultDOMEvent(notification,
                            (org.opendaylight.mdsal.dom.api.DOMEvent)notification));
                        return;
                    }

                    listener.onNotification(new DefaultDOMNotification(notification));
                }, types);

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
    protected org.opendaylight.mdsal.dom.api.DOMNotificationService delegate() {
        return delegate;
    }

    private static class DefaultDOMNotification implements DOMNotification {
        private final org.opendaylight.mdsal.dom.api.DOMNotification delegate;

        DefaultDOMNotification(final org.opendaylight.mdsal.dom.api.DOMNotification delegate) {
            this.delegate = requireNonNull(delegate);
        }

        @Override
        public SchemaPath getType() {
            return delegate.getType();
        }

        @Override
        public ContainerNode getBody() {
            return delegate.getBody();
        }
    }

    private static class DefaultDOMEvent extends DefaultDOMNotification implements DOMEvent {
        private final Date eventTime;

        DefaultDOMEvent(final org.opendaylight.mdsal.dom.api.DOMNotification fromNotification,
                final org.opendaylight.mdsal.dom.api.DOMEvent fromEvent) {
            super(fromNotification);
            final Instant eventInstant = fromEvent.getEventInstant();
            this.eventTime = eventInstant != null ? Date.from(eventInstant) : null;
        }

        @Override
        public Date getEventTime() {
            return eventTime;
        }
    }
}
