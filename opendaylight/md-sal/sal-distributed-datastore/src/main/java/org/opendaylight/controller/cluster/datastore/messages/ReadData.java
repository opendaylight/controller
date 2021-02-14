/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ReadData extends AbstractRead<Optional<NormalizedNode>> {
    private static final long serialVersionUID = 1L;

    public ReadData() {
    }

    public ReadData(final YangInstanceIdentifier path, final short version) {
        super(path, version);
    }

    @Override
    public FluentFuture<Optional<NormalizedNode>> apply(final DOMStoreReadTransaction readDelegate) {
        return readDelegate.read(getPath());
    }

    @Override
    public void processResponse(final Object readResponse,
            final SettableFuture<Optional<NormalizedNode>> returnFuture) {
        if (ReadDataReply.isSerializedType(readResponse)) {
            ReadDataReply reply = ReadDataReply.fromSerializable(readResponse);
            returnFuture.set(Optional.ofNullable(reply.getNormalizedNode()));
        } else {
            returnFuture.setException(new ReadFailedException("Invalid response reading data for path " + getPath()));
        }
    }

    @Override
    protected AbstractRead<Optional<NormalizedNode>> newInstance(final short withVersion) {
        return new ReadData(getPath(), withVersion);
    }

    public static ReadData fromSerializable(final Object serializable) {
        Preconditions.checkArgument(serializable instanceof ReadData);
        return (ReadData)serializable;
    }

    public static boolean isSerializedType(final Object message) {
        return message instanceof ReadData;
    }
}
