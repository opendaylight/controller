/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

/**
 * Abstract base class for serialization proxies associated with {@link AbstractReadTransactionRequest}s. It implements
 * the initial (Boron) serialization format.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
abstract class AbstractReadTransactionRequestProxyV1<T extends AbstractReadTransactionRequest<T>>
        extends AbstractTransactionRequestProxy<T> implements AbstractReadTransactionRequest.SerialForm<T> {
    private static final long serialVersionUID = 1L;

    protected AbstractReadTransactionRequestProxyV1() {
        // For Externalizable
    }

    AbstractReadTransactionRequestProxyV1(final T request) {
        super(request);
    }
}
