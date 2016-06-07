/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestProxy;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

/**
 * Abstract base class for serialization proxies associated with {@link LocalHistoryRequest}s.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
abstract class AbstractLocalHistoryRequestProxy<T extends LocalHistoryRequest<T>> extends AbstractRequestProxy<LocalHistoryIdentifier, T> {
    private static final long serialVersionUID = 1L;

    AbstractLocalHistoryRequestProxy() {
        // For Externalizable
    }

    AbstractLocalHistoryRequestProxy(final T request) {
        super(request);
    }

    @Override
    protected final LocalHistoryIdentifier readTarget(final DataInput in) throws IOException {
        return LocalHistoryIdentifier.readFrom(in);
    }
}
