/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.io.IOException;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeDataOutput;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * An individual modification of a transaction's state. This class and its subclasses are not serializable, but rather
 * expose {@link #writeTo(NormalizedNodeDataOutput)} and {@link #readFrom(NormalizedNodeDataInput)} methods for explicit
 * serialization. The reason for this is that they are usually transmitted in bulk, hence it is advantageous to reuse
 * a {@link NormalizedNodeDataOutput} instance to achieve better compression.
 *
 * @author Robert Varga
 */
@Beta
public abstract class TransactionModification {
    static final byte TYPE_DELETE = 1;
    static final byte TYPE_MERGE = 2;
    static final byte TYPE_WRITE = 3;

    private final YangInstanceIdentifier path;

    TransactionModification(final YangInstanceIdentifier path) {
        this.path = Preconditions.checkNotNull(path);
    }

    public final YangInstanceIdentifier getPath() {
        return path;
    }

    abstract byte getType();

    void writeTo(final NormalizedNodeDataOutput out) throws IOException {
        out.writeByte(getType());
        out.writeYangInstanceIdentifier(path);
    }

    static TransactionModification readFrom(final NormalizedNodeDataInput in) throws IOException {
        final byte type = in.readByte();
        switch (type) {
            case TYPE_DELETE:
                return new TransactionDelete(in.readYangInstanceIdentifier());
            case TYPE_MERGE:
                return new TransactionMerge(in.readYangInstanceIdentifier(), in.readNormalizedNode());
            case TYPE_WRITE:
                return new TransactionWrite(in.readYangInstanceIdentifier(), in.readNormalizedNode());
            default:
                throw new IllegalArgumentException("Unhandled type " + type);
        }
    }
}
