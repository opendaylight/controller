/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

/**
 * NormalizedNodeOutputStreamWriter for Neon SR3 streaming format. In addition to normal output, this
 */
class NeonSR3NormalizedNodeOutputStreamWriter extends NeonSR2NormalizedNodeOutputStreamWriter {
    private final Deque<PathArgument> path = new ArrayDeque<>();

    private boolean scalarEnabled;

    NeonSR3NormalizedNodeOutputStreamWriter(final DataOutput output) {
        super(output);
    }

    @Override
    protected short streamVersion() {
        return TokenTypes.NEON_SR3_VERSION;
    }

    @Override
    public void startLeafNode(final NodeIdentifier name) throws IOException {
        final PathArgument current = path.peek();
        if (current instanceof NodeIdentifierWithPredicates &&
                ((NodeIdentifierWithPredicates) current).getValue(name.getNodeType()) != null) {
            startLeafNode(name, NodeTypes.KEY_LEAF);
            scalarEnabled = false;
        } else {
            super.startLeafNode(name);
        }
    }

    @Override
    public void endNode() throws IOException {
        super.endNode();
        path.pop();
        scalarEnabled = true;
    }

    @Override
    public void scalarValue(final Object value) throws IOException {
        if (scalarEnabled) {
            super.scalarValue(value);
        }
    }

    @Override
    void startNode(final PathArgument arg, final byte nodeType) throws IOException {
        super.startNode(arg, nodeType);
        path.push(arg);
    }
}
