/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListenerRegistration;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;

/**
 * Utility base class for {@link DOMNotificationListenerRegistration}
 * implementations.
 */
public abstract class AbstractDOMNotificationListenerRegistration extends AbstractListenerRegistration<DOMNotificationListener> implements DOMNotificationListenerRegistration {
    /**
     * Default constructor. Subclasses need to invoke it from their
     * constructor(s).
     *
     * @param listener {@link DOMNotificationListener} instance which is
     *                 being held by this registration. May not be null.
     */
    protected AbstractDOMNotificationListenerRegistration(final @Nonnull DOMNotificationListener listener) {
        super(listener);
    }
}
