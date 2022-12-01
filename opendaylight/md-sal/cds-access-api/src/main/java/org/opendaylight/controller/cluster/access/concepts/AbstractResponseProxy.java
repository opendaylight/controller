/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Abstract Externalizable proxy class to use with {@link Response} subclasses.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
abstract class AbstractResponseProxy<T extends WritableIdentifier, C extends Response<T, C>>
        extends AbstractMessageProxy<T, C> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    protected AbstractResponseProxy() {
        // for Externalizable
    }

    AbstractResponseProxy(final @NonNull C response) {
        super(response);
    }
}
