/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connect.netconf.sal;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

class NetconfDeviceNotificationService implements DOMNotificationService {

    private final Multimap<SchemaPath, DOMNotificationListener> listeners = HashMultimap.create();

    // Notification publish is very simple and hijacks the thread of the caller
    // TODO shouldnt we reuse the implementation for notification router from sal-broker-impl ?
    public synchronized void publishNotification(final ContainerNode notification) {
        final SchemaPath schemaPath = toPath(notification.getNodeType());
        for (final DOMNotificationListener domNotificationListener : listeners.get(schemaPath)) {
            domNotificationListener.onNotification(new DOMNotification() {
                @Nonnull
                @Override
                public SchemaPath getType() {
                    return schemaPath;
                }

                @Nonnull
                @Override
                public ContainerNode getBody() {
                    return notification;
                }
            });
        }
    }

    @Override
    public synchronized <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(@Nonnull final T listener, @Nonnull final Collection<SchemaPath> types) {
        for (final SchemaPath type : types) {
            listeners.put(type, listener);
        }

        // FIXME this should invoke create-subscription rpc on the remote device for a given notification

        return new ListenerRegistration<T>() {
            @Override
            public void close() {
                for (final SchemaPath type : types) {
                    listeners.remove(type, listener);
                }
            }

            @Override
            public T getInstance() {
                return listener;
            }
        };
    }

    @Override
    public synchronized <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(@Nonnull final T listener, final SchemaPath... types) {
        return registerNotificationListener(listener, Lists.newArrayList(types));
    }
}
