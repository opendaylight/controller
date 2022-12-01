/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxy;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

/**
 * Abstract base class for serialization proxies associated with {@link LocalHistoryRequest}s.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
abstract class AbstractLocalHistoryRequestProxy<T extends LocalHistoryRequest<T>>
        extends AbstractRequestProxy<LocalHistoryIdentifier, T> implements LocalHistoryRequest.SerialForm<T> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    protected AbstractLocalHistoryRequestProxy() {
        // For Externalizable
    }

    AbstractLocalHistoryRequestProxy(final T request) {
        super(request);
    }
}
