/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.registration;

import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

/**
 * Registration of EventSource have to be provided by object implements this interface
 * @author madamjak
 *
 * @param <T>
 */
public interface EventSourceRegistry<T> extends AutoCloseable {
    /**
     * implementation has to do all necessary procedures to register given event source
     * @param es
     * @return
     */
    AbstractObjectRegistration<EventSource<T>> registerEventSource(EventSource<T> es);

}
