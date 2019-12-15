/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.modification;

import java.io.IOException;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.ReusableStreamReceiver;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput;

/**
 * MergeModification stores all the parameters required to merge data into the specified path.
 */
public class MergeModification extends WriteModification {
    private static final long serialVersionUID = 1L;

    public MergeModification() {
        this(DataStoreVersions.CURRENT_VERSION);
    }

    public MergeModification(final short version) {
        super(version);
    }

    public MergeModification(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        super(path, data);
    }

    MergeModification(final short version, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        super(version, path, data);
    }

    @Override
    public void apply(final DOMStoreWriteTransaction transaction) {
        transaction.merge(getPath(), getData());
    }

    @Override
    public void apply(final DataTreeModification transaction) {
        transaction.merge(getPath(), getData());
    }

    @Override
    public byte getType() {
        return MERGE;
    }

    public static MergeModification fromStream(final NormalizedNodeDataInput in, final short version,
            final ReusableStreamReceiver receiver) throws IOException {
        final NormalizedNode<?, ?> node = in.readNormalizedNode(receiver);
        final YangInstanceIdentifier path = in.readYangInstanceIdentifier();
        return new MergeModification(version, path, node);
    }
}
