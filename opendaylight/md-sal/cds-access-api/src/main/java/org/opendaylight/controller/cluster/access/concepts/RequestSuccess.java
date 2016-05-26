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
import org.opendaylight.yangtools.concepts.Identifier;

@Beta
public abstract class RequestSuccess<T extends Identifier & WritableObject> extends Response<T, RequestSuccess<T>> {
    protected static abstract class SuccessProxy<T extends Identifier & WritableObject> extends
            ResponseProxy<T, RequestSuccess<T>> implements Externalizable {

        public SuccessProxy() {
            // For Externalizable
        }

        protected SuccessProxy(final T target, final long sequence) {
            super(target, sequence);
        }

        @Override
        protected final Response<T, RequestSuccess<T>> createResponse(final T target, final long sequence) {
            return createSuccess(target, sequence);
        }

        protected abstract RequestSuccess<T> createSuccess(T target, long sequence);
    }

    private static final long serialVersionUID = 1L;

    protected RequestSuccess(final T target, final long sequence) {
        super(target, sequence);
    }

    @Override
    protected abstract SuccessProxy<T> externalizableProxy();
}
