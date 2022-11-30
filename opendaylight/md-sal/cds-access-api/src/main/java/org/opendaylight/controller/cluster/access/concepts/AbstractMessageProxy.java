/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Abstract Externalizable proxy for use with {@link Message} subclasses.
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
@Deprecated(since = "7.0.0", forRemoval = true)
abstract class AbstractMessageProxy<T extends WritableIdentifier, C extends Message<T, C>>
        implements Message.SerialForm<T, C> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private C message;

    protected AbstractMessageProxy() {
        // For Externalizable
    }

    AbstractMessageProxy(final @NonNull C message) {
        this.message = requireNonNull(message);
    }

    @Override
    public final C message() {
        return verifyNotNull(message);
    }

    @Override
    public final void resolveTo(final C newMessage) {
        this.message = requireNonNull(newMessage);
    }

    @Override
    public final Object readResolve() {
        return verifyNotNull(message);
    }
}
