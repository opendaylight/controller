/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;

/**
 * Abstract implementation of a ListenerRegistration constrained to subclasses
 * of {@link DOMDataTreeChangeListener}.
 *
 * @param <T> type of listener
 */
public abstract class AbstractDOMDataTreeChangeListenerRegistration<T extends DOMDataTreeChangeListener> extends AbstractListenerRegistration<T> {
    protected AbstractDOMDataTreeChangeListenerRegistration(final T listener) {
        super(listener);
    }
}
