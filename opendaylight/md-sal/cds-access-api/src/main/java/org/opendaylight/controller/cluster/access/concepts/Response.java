/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Abstract counterpart to a {@link Request}. This class should not be instantiated directly, but rather through
 * {@link RequestFailure} and {@link RequestSuccess}, which provide appropriate specialization. It is visible purely for
 * the purpose of allowing to check if an object is either of those specializations with a single instanceof check.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 * @param <C> Message type
 */
@Beta
public abstract class Response<T extends WritableIdentifier, C extends Response<T, C>> extends Message<T, C> {
    private static final long serialVersionUID = 1L;

    Response(final @Nonnull T target, final long sequence) {
        super(target, sequence);
    }

    Response(final @Nonnull C response, final @Nonnull ABIVersion version) {
        super(response, version);
    }

    @Override
    abstract AbstractResponseProxy<T, C> externalizableProxy(@Nonnull ABIVersion version);
}
