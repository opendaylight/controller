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
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * A failure response to a {@link Request}. Contains a {@link RequestException} detailing the cause for this failure.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 * @param <C> Message class
 */
@Beta
public abstract class RequestFailure<T extends WritableIdentifier, C extends RequestFailure<T, C>> extends Response<T, C> {
    private static final long serialVersionUID = 1L;
    private final RequestException cause;

    protected RequestFailure(final @Nonnull C failure, final @Nonnull ABIVersion version) {
        super(failure, version);
        this.cause = Preconditions.checkNotNull(failure.getCause());
    }

    protected RequestFailure(final @Nonnull T target, final long sequence, final @Nonnull RequestException cause) {
        super(target, sequence);
        this.cause = Preconditions.checkNotNull(cause);
    }

    /**
     * Return the failure cause.
     *
     * @return Failure cause.
     */
    public final @Nonnull RequestException getCause() {
        return cause;
    }

    /**
     * Return an indication of whether this a hard failure. Hard failures must not be retried but need to be treated
     * as authoritative response to a request.
     *
     * @return True if this represents a hard failure, false otherwise.
     */
    public final boolean isHardFailure() {
        return !cause.isRetriable();
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("cause", cause);
    }

    @Override
    protected abstract AbstractRequestFailureProxy<T, C> externalizableProxy(@Nonnull ABIVersion version);
}
