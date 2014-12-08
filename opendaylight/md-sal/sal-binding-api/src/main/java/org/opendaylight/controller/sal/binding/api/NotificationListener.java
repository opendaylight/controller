/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import java.util.EventListener;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * Interface for a generic listener that is interested in receiving YANG modeled notifications.
 * This interface acts as a base interface for specific listeners which usually are a type
 * capture of this interface.
 *
 * @param <T> the interested notification type
 * @deprecated Deprecated unused API.
 */
@Deprecated
public interface NotificationListener<T extends Notification> extends EventListener {
    /**
     * Invoked to deliver a notification.
     * <p>
     * Note that this method may be invoked from a shared thread pool, so implementations SHOULD NOT
     * perform CPU-intensive operations and MUST NOT invoke any potentially blocking operations.
     *
     * @param notification the notification.
     */
    void onNotification(T notification);
}
