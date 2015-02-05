/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * A {@link DOMService} which allows its users to subscribe to receive
 * {@link DOMNotification}s.
 */
public interface DOMNotificationService {
    /**
     * Register a {@link DOMNotificationListener} to receive a set of notifications. As with
     * other ListenerRegistration-based interfaces, registering an instance multiple times
     * results in notifications being delivered for each registration.
     *
     * @param listener Notification instance to register
     * @param types Notification types which should be delivered to the listener. Duplicate
     *              entries are processed only once, null entries are ignored.
     * @return Registration handle. Invoking {@link DOMNotificationListenerRegistration#close()}
     *         will stop the delivery of notifications to the listener
     * @throws IllegalArgumentException if types is empty or contains an invalid element, such as
     *         null or a SchemaPath which does not represent a valid {@link DOMNotification} type.
     * @throws NullPointerException if either of the arguments is null
     */
    <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(@Nonnull T listener, @Nonnull Collection<SchemaPath> types);

    /**
     * Register a {@link DOMNotificationListener} to receive a set of notifications. As with
     * other ListenerRegistration-based interfaces, registering an instance multiple times
     * results in notifications being delivered for each registration.
     *
     * @param listener Notification instance to register
     * @param types Notification types which should be delivered to the listener. Duplicate
     *              entries are processed only once, null entries are ignored.
     * @return Registration handle. Invoking {@link DOMNotificationListenerRegistration#close()}
     *         will stop the delivery of notifications to the listener
     * @throws IllegalArgumentException if types is empty or contains an invalid element, such as
     *         null or a SchemaPath which does not represent a valid {@link DOMNotification} type.
     * @throws NullPointerException if listener is null
     */
    // FIXME: Java 8: provide a default implementation of this method.
    <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(@Nonnull T listener, SchemaPath... types);
}
