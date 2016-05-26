/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import org.opendaylight.yangtools.concepts.Identifier;

/**
 * Abstract Externalizable proxy class to use with {@link Response} subclasses.
 * @param <T> Target identifier type
 * @param <C> Message class
 *
 * @author Robert Varga
 */
abstract class AbstractResponseProxy<T extends Identifier & WritableObject, C extends Response<T, C>>
        extends AbstractMessageProxy<T, C> {
    private static final long serialVersionUID = 1L;

    public AbstractResponseProxy() {
        // for Externalizable
    }

    AbstractResponseProxy(final Response<T, C> response) {
        super(response);
    }

    @Override
    final Response<T, C> createMessage(final T target, final long sequence) {
        return createResponse(target, sequence);
    }

    abstract Response<T, C> createResponse(T target, long sequence);
}