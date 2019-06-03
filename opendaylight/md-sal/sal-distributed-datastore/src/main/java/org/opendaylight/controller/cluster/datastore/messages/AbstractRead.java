/*
 * Copyright (c) 2015 Huawei, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
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
    public final void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        path = SerializationUtils.readPath(in);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        SerializationUtils.writePath(out, getStreamVersion(), path);
    }

    public AbstractRead<T> asVersion(final short version) {
        return version == getVersion() ? this : newInstance(version);
    }

    public abstract FluentFuture<T> apply(DOMStoreReadTransaction readDelegate);

    public abstract void processResponse(Object reponse, SettableFuture<T> promise);

    protected abstract AbstractRead<T> newInstance(short withVersion);
}
