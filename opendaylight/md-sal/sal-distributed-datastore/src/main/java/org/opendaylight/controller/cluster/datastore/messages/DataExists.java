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
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

@Deprecated(since = "9.0.0", forRemoval = true)
public class DataExists extends AbstractRead<Boolean> {
    private static final long serialVersionUID = 1L;

    public DataExists() {
    }

    public DataExists(final YangInstanceIdentifier path, final short version) {
        super(path, version);
    }

    @Override
    public FluentFuture<Boolean> apply(final DOMStoreReadTransaction readDelegate) {
        return readDelegate.exists(getPath());
    }

    @Override
    public void processResponse(final Object response, final SettableFuture<Boolean> returnFuture) {
        if (DataExistsReply.isSerializedType(response)) {
            returnFuture.set(Boolean.valueOf(DataExistsReply.fromSerializable(response).exists()));
        } else {
            returnFuture.setException(new ReadFailedException("Invalid response checking exists for path "
                    + getPath()));
        }
    }

    @Override
    protected AbstractRead<Boolean> newInstance(final short withVersion) {
        return new DataExists(getPath(), withVersion);
    }

    public static DataExists fromSerializable(final Object serializable) {
        Preconditions.checkArgument(serializable instanceof DataExists);
        return (DataExists)serializable;
    }

    public static boolean isSerializedType(final Object message) {
        return message instanceof DataExists;
    }
}
