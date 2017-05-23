/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
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
 * <p>
 * This class is visible outside of this package for the purpose of allowing common instanceof checks
 * and simplified codepaths.
 *
 * @author Robert Varga
 *
 * @param <T> Message type
 */
@Beta
public abstract class AbstractReadPathTransactionRequest<T extends AbstractReadPathTransactionRequest<T>>
        extends AbstractReadTransactionRequest<T> {
    private static final long serialVersionUID = 1L;
    private final YangInstanceIdentifier path;

    AbstractReadPathTransactionRequest(final TransactionIdentifier identifier, final long sequence,
        final ActorRef replyTo, final YangInstanceIdentifier path, final boolean snapshotOnly) {
        super(identifier, sequence, replyTo, snapshotOnly);
        this.path = Preconditions.checkNotNull(path);
    }

    AbstractReadPathTransactionRequest(final T request, final ABIVersion version) {
        super(request, version);
        this.path = request.getPath();
    }

    @Nonnull
    public final YangInstanceIdentifier getPath() {
        return path;
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("path", path);
    }

    @Override
    protected abstract AbstractReadTransactionRequestProxyV1<T> externalizableProxy(ABIVersion version);
}
