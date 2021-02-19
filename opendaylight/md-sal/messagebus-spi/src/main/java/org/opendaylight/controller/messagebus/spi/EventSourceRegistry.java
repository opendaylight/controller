/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.spi;

/**
 * EventSourceRegistry is used to register {@link EventSource}.
 */
@Deprecated(forRemoval = true)
public interface EventSourceRegistry extends AutoCloseable {

    /**
     * Registers the given EventSource for public consumption. The EventSource is
     * associated with the node identified via {@link EventSource#getSourceNodeKey}.
     *
     * @param eventSource the EventSource instance to register
     * @return an EventSourceRegistration instance that is used to unregister the EventSource
     *         via {@link EventSourceRegistration#close()}.
     */
    <T extends EventSource> EventSourceRegistration<T> registerEventSource(T eventSource);
}
