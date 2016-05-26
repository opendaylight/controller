/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.concepts.Identifier;

@Beta
public abstract class Response<T extends Identifier & WritableObject, C extends Response<T, C>> extends Message<T, C> {
    static abstract class ResponseProxy<T extends Identifier & WritableObject, C extends
        Response<T, C>> extends AbstractProxy<T, C> {

        public ResponseProxy() {
            // for Externalizable
        }

        ResponseProxy(final T target, final long sequence) {
            super(target, sequence);
        }

        @Override
        protected final Response<T, C> createMessage(final T target, final long sequence) {
            return createResponse(target, sequence);
        }

        abstract Response<T, C> createResponse(T target, long sequence);
    }

    private static final long serialVersionUID = 1L;

    Response(final T target, final long sequence) {
        super(target, sequence);
    }

    @Override
    abstract ResponseProxy<T, C> externalizableProxy();
}
