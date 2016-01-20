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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Abstract base class for ReadData and DataExists messages.
 *
 * @author gwu
 *
 */
public abstract class AbstractRead<T> extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private YangInstanceIdentifier path;

    protected AbstractRead() {
    }

    public AbstractRead(final YangInstanceIdentifier path, final short version) {
        super(version);
        this.path = path;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        path = SerializationUtils.deserializePath(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        SerializationUtils.serializePath(path, out);
    }

    public AbstractRead<T> asVersion(short version) {
        return version == getVersion() ? this : newInstance(version);
    }

    public abstract CheckedFuture<T, ReadFailedException> apply(DOMStoreReadTransaction readDelegate);

    public abstract void processResponse(Object reponse, SettableFuture<T> promise);

    protected abstract AbstractRead<T> newInstance(short withVersion);
}
