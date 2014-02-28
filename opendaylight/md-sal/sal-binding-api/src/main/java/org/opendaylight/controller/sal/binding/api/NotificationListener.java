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
 * Interface implemented by objects interested in some sort of Notification. This
 * class acts as a base interface for specific listeners which usually are a type
 * capture of this interface.
 *
 * @param <T> Notification type
 */
public interface NotificationListener<T extends Notification> extends EventListener {
	/**
	 * Invoked to deliver the notification. Note that this method may be invoked
	 * from a shared thread pool, so implementations SHOULD NOT perform CPU-intensive
	 * operations and they definitely MUST NOT invoke any potentially blocking
	 * operations.
	 *
	 * @param notification Notification being delivered.
	 */
    void onNotification(T notification);
}
