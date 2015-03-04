/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.registration;

import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

public abstract class EventSourceRegistration<T> extends AbstractObjectRegistration<EventSource<T>> {

    protected EventSourceRegistration(EventSource<T> instance) {
        super(instance);
    }

}
