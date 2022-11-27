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
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
abstract class AbstractMessageProxy<T extends WritableIdentifier, C extends Message<T, C>>
        implements Message.SerialForm<T, C> {
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
    public final void setMessage(final C message) {
        this.message = requireNonNull(message);
    }

    @Override
    public final Object readResolve() {
        return message();
    }
}
