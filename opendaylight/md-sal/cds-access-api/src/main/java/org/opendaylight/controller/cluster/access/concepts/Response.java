/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Abstract counterpart to a {@link Request}. This class should not be instantiated directly, but rather through
 * {@link RequestFailure} and {@link RequestSuccess}, which provide appropriate specialization. It is visible purely for
 * the purpose of allowing to check if an object is either of those specializations with a single instanceof check.
 *
 * @param <T> Target identifier type
 * @param <C> Message type
 */
public abstract class Response<T extends WritableIdentifier, C extends Response<T, C>> extends Message<T, C> {
    protected interface SerialForm<T extends WritableIdentifier, C extends Response<T, C>>
            extends Message.SerialForm<T, C> {

    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    Response(final @NonNull T target, final long sequence) {
        super(target, sequence);
    }

    Response(final @NonNull C response, final @NonNull ABIVersion version) {
        super(response, version);
    }
}
