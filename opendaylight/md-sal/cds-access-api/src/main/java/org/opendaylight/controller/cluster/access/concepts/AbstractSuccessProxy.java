/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import java.io.Externalizable;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Abstract Externalizable proxy for use with {@link RequestSuccess} subclasses.
 *
 * @author Robert Varga
 *
 * @param <T> Target identifier type
 */
@Beta
public abstract class AbstractSuccessProxy<T extends WritableIdentifier, C extends RequestSuccess<T, C>>
        extends AbstractResponseProxy<T, C> implements Externalizable {
    private static final long serialVersionUID = 1L;

    protected AbstractSuccessProxy() {
        // For Externalizable
    }

    protected AbstractSuccessProxy(final @Nonnull C success) {
        super(success);
    }

    @Override
    final C createResponse(final T target, final long sequence) {
        return createSuccess(target, sequence);
    }

    protected abstract @Nonnull C createSuccess(@Nonnull T target, long sequence);
}