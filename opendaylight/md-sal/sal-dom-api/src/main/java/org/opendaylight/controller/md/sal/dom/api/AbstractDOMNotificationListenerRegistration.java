/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;

public abstract class AbstractDOMNotificationListenerRegistration extends AbstractListenerRegistration<DOMNotificationListener> {
    protected AbstractDOMNotificationListenerRegistration(final DOMNotificationListener listener) {
        super(listener);
    }
}
