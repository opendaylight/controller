/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * A failure response to a {@link Request}. Contains a {@link RequestException} detailing the cause for this failure.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 */
@Beta
public abstract class RequestFailure<T extends Identifier & WritableObject> extends Response<T, RequestFailure<T>> {
    private static final long serialVersionUID = 1L;
    private final RequestException cause;

    protected RequestFailure(final T target, final long sequence, final RequestException cause) {
        super(target, sequence);
        this.cause = Preconditions.checkNotNull(cause);
    }

    /**
     * Return the failure cause.
     *
     * @return Failure cause.
     */
    public final RequestException getCause() {
        return cause;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("cause", cause);
    }

    @Override
    protected abstract AbstractRequestFailureProxy<T> externalizableProxy();
}
