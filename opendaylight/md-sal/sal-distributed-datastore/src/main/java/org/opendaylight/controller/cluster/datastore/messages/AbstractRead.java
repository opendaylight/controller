/*
 * Copyright (c) 2015 Huawei, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Abstract base class for ReadData and DataExists messages.
 *
 * @author gwu
 *
 */
public abstract class AbstractRead<T> implements SerializableMessage {
    private final YangInstanceIdentifier path;

    public AbstractRead(final YangInstanceIdentifier path) {
        this.path = path;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public abstract CheckedFuture<T, ReadFailedException> apply(DOMStoreReadTransaction readDelegate);

    public abstract void processResponse(Object reponse, SettableFuture<T> promise);

}
