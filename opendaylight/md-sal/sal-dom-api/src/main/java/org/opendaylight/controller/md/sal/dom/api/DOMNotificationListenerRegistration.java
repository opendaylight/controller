/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * A registration of a {@link DOMNotificationListener}. Invoking {@link #close()} will prevent further
 * delivery of events to the listener.
 */
public interface DOMNotificationListenerRegistration extends ListenerRegistration<DOMNotificationListener> {

}
