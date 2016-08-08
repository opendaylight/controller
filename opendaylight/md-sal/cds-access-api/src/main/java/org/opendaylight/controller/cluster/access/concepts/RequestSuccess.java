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
 * A successful reply to a {@link Request}.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 */
@Beta
public abstract class RequestSuccess<T extends WritableIdentifier, C extends RequestSuccess<T, C>> extends
        Response<T, C> {
    private static final long serialVersionUID = 1L;

    protected RequestSuccess(final @Nonnull C success, final @Nonnull ABIVersion version) {
        super(success, version);
    }

    protected RequestSuccess(final @Nonnull T target, final long sequence) {
        super(target, sequence);
    }

    @Override
    protected abstract AbstractSuccessProxy<T, C> externalizableProxy(@Nonnull ABIVersion version);
}
