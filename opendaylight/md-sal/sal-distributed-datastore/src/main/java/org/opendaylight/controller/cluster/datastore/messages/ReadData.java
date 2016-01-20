/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.cluster.datastore.util.InstanceIdentifierUtils;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ReadData extends AbstractRead<Optional<NormalizedNode<?, ?>>> {
    public static final Class<ShardTransactionMessages.ReadData> SERIALIZABLE_CLASS = ShardTransactionMessages.ReadData.class;

    public ReadData(final YangInstanceIdentifier path) {
        super(path);
    }

    @Override
    public Object toSerializable() {
        return ShardTransactionMessages.ReadData.newBuilder()
                .setInstanceIdentifierPathArguments(InstanceIdentifierUtils.toSerializable(getPath())).build();
    }

    public static ReadData fromSerializable(final Object serializable) {
        ShardTransactionMessages.ReadData o = (ShardTransactionMessages.ReadData)serializable;
        return new ReadData(InstanceIdentifierUtils.fromSerializable(o.getInstanceIdentifierPathArguments()));
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> apply(DOMStoreReadTransaction readDelegate) {
        return readDelegate.read(getPath());
    }

    @Override
    public void processResponse(Object readResponse, SettableFuture<Optional<NormalizedNode<?, ?>>> returnFuture) {
        if(ReadDataReply.isSerializedType(readResponse)) {
            ReadDataReply reply = ReadDataReply.fromSerializable(readResponse);
            returnFuture.set(Optional.<NormalizedNode<?, ?>> fromNullable(reply.getNormalizedNode()));
        } else {
            returnFuture.setException(new ReadFailedException("Invalid response reading data for path " + getPath()));
        }
    }
}
