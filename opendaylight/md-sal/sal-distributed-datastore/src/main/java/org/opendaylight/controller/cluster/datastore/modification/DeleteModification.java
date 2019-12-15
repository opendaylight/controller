/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput;

/**
 * DeleteModification store all the parameters required to delete a path from the data tree.
 */
public class DeleteModification extends AbstractModification {
    private static final long serialVersionUID = 1L;

    public DeleteModification() {
        this(DataStoreVersions.CURRENT_VERSION);
    }

    public DeleteModification(final short version) {
        super(version);
    }

    public DeleteModification(final YangInstanceIdentifier path) {
        super(path);
    }

    DeleteModification(final short version, final YangInstanceIdentifier path) {
        super(version, path);
    }

    @Override
    public void apply(final DOMStoreWriteTransaction transaction) {
        transaction.delete(getPath());
    }

    @Override
    public void apply(final DataTreeModification transaction) {
        transaction.delete(getPath());
    }

    @Override
    public byte getType() {
        return DELETE;
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        setPath(SerializationUtils.readPath(in));
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        SerializationUtils.writePath(out, getPath());
    }

    @Override
    public void writeTo(final NormalizedNodeDataOutput out) throws IOException {
        out.writeYangInstanceIdentifier(getPath());
    }

    public static DeleteModification fromStream(final NormalizedNodeDataInput in, final short version)
            throws IOException {
        return new DeleteModification(version, in.readYangInstanceIdentifier());
    }
}
