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
public abstract class RequestSuccess<T extends Identifier> extends Response<T, RequestSuccess<T>> {
    protected static abstract class AbstractSuccessProxy<T extends Identifier> extends AbstractProxy<T, RequestSuccess<T>> implements Externalizable {
        public AbstractSuccessProxy() {
            // For Externalizable
        }

        protected AbstractSuccessProxy(final T identifier) {
            super(identifier);
        }

        @Override
        protected abstract RequestSuccess<T> readResolve();
    }

    private static final long serialVersionUID = 1L;

    protected RequestSuccess(final T identifier) {
        super(identifier);
    }

    @Override
    protected abstract AbstractSuccessProxy<T> writeReplace();
}
