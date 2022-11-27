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
import org.opendaylight.yangtools.concepts.WritableIdentifier;

/**
 * Abstract Externalizable proxy for use with {@link Request} subclasses.
 *
 * @param <T> Target identifier type
 */
public abstract class AbstractRequestProxy<T extends WritableIdentifier, C extends Request<T, C>>
        extends AbstractMessageProxy<T, C> implements RequestProxy<T, C> {
    @Serial
    private static final long serialVersionUID = 1L;

    protected AbstractRequestProxy() {
        // For Externalizable
    }

    protected AbstractRequestProxy(final @NonNull C request) {
        super(request);
    }
}
