/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractCheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;

/**
 * A {@link Future} used to report the status of an future {@link java.util.concurrent.Future}.
 */
final class PingPongFuture extends AbstractCheckedFuture<Void, TransactionCommitFailedException> {
    protected PingPongFuture(final ListenableFuture<Void> delegate) {
        super(delegate);
    }

    @Override
    protected TransactionCommitFailedException mapException(final Exception e) {
        Preconditions.checkArgument(e instanceof TransactionCommitFailedException);
        return (TransactionCommitFailedException) e;
    }
}
