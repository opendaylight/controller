/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Abstract base class for {@link TransactionRequest}s accessing data as visible in the isolated context of a particular
 * transaction. The path of the data being accessed is returned via {@link #getPath()}.
 *
 * This class is visible outside of this package for the purpose of allowing common instanceof checks
 * and simplified codepaths.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
@Beta
public abstract class AbstractReadTransactionRequest<T extends AbstractReadTransactionRequest<T>> extends TransactionRequest<T> {
    private static final long serialVersionUID = 1L;
    private final YangInstanceIdentifier path;

    AbstractReadTransactionRequest(final TransactionIdentifier identifier, final long sequence, final ActorRef replyTo,
        final YangInstanceIdentifier path) {
        super(identifier, sequence, replyTo);
        this.path = Preconditions.checkNotNull(path);
    }

    AbstractReadTransactionRequest(final T request, final ABIVersion version) {
        super(request, version);
        this.path = request.getPath();
    }

    public final @Nonnull YangInstanceIdentifier getPath() {
        return path;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("path", path);
    }

    @Override
    protected abstract AbstractReadTransactionRequestProxyV1<T> externalizableProxy(final ABIVersion version);
}
