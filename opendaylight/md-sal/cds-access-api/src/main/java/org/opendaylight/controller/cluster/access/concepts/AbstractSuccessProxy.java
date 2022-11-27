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
 * Abstract Externalizable proxy for use with {@link RequestSuccess} subclasses.
 *
 * @param <T> Target identifier type
 */
public abstract class AbstractSuccessProxy<T extends WritableIdentifier, C extends RequestSuccess<T, C>>
        extends AbstractResponseProxy<T, C> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    protected AbstractSuccessProxy() {
        // For Externalizable
    }

    protected AbstractSuccessProxy(final @NonNull C success) {
        super(success);
    }
}
