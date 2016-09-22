/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.spi;

import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Instance of EventSourceRegistration is returned by {@link EventSourceRegistry#registerEventSource(EventSource)}
 * and it is used to unregister EventSource.
 *
 */
public interface EventSourceRegistration <T extends EventSource> extends ObjectRegistration<T>{

    @Override
    void close();

}
