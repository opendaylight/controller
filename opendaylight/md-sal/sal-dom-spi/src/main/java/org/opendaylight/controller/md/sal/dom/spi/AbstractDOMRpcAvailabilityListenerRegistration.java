/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListenerRegistration;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;

/**
 * Abstract base class for {@link DOMRpcAvailabilityListenerRegistration} implementations.
 */
public abstract class AbstractDOMRpcAvailabilityListenerRegistration extends AbstractListenerRegistration<DOMRpcAvailabilityListener> implements DOMRpcAvailabilityListenerRegistration {
    protected AbstractDOMRpcAvailabilityListenerRegistration(final DOMRpcAvailabilityListener listener) {
        super(listener);
    }
}
