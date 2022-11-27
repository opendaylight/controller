/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import java.io.Serial;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A successful reply to a {@link Request}.
 *
 * @param <T> Target identifier type
 */
public abstract class RequestSuccess<T extends WritableIdentifier, C extends RequestSuccess<T, C>>
        extends Response<T, C> {
    @Serial
    private static final long serialVersionUID = 1L;

    protected RequestSuccess(final @NonNull C success,  final @NonNull ABIVersion version) {
        super(success, version);
    }

    protected RequestSuccess(final @NonNull T target, final long sequence) {
        super(target, sequence);
    }

    @Override
    protected abstract AbstractSuccessProxy<T, C> externalizableProxy(ABIVersion version);
}
