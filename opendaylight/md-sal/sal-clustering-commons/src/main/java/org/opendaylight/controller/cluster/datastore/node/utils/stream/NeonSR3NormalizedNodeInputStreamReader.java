/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;

final class NeonSR3NormalizedNodeInputStreamReader extends NeonSR2NormalizedNodeInputStreamReader {
    NeonSR3NormalizedNodeInputStreamReader(final DataInput input) {
        super(input);
    }

    @Override
    void streamMapChild(final NormalizedNodeStreamWriter writer, final NodeIdentifierWithPredicates entryIid,
            final byte nodeType) throws IOException {
        if (nodeType == NodeTypes.KEY_LEAF) {
            // ... key leaves do not have a value
            streamKeyLeaf(writer, entryIid);
        } else {
            streamNormalizedNode(writer, nodeType);
        }
    }

    // Key leaf inside a MapEntryNode, value is picked up from entry identifier
    private void streamKeyLeaf(final NormalizedNodeStreamWriter writer, final NodeIdentifierWithPredicates entryId)
            throws IOException {
        final NodeIdentifier identifier = startLeaf(writer);
        final Object value = entryId.getValue(identifier.getNodeType());
        if (value == null) {
            throw new IOException("Value for key leaf " + identifier + " not found in " + entryId);
        }
        endLeaf(writer, value);
    }
}
