
/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.stream;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Event Stream Writer based on Normalized Node tree representation
 *
 * <h3>Writing Event Stream</h3>
 *
 * <ul>
 * <li><code>container</code> - Container node representation, start event is
 * emitted using {@link #startContainerNode(YangInstanceIdentifier.NodeIdentifier, int)}
 * and node end event is
 * emitted using {@link #endNode()}. Container node is implementing
 * {@link org.opendaylight.yangtools.yang.binding.DataObject} interface.
 *
 * <li><code>list</code> - YANG list statement has two representation in event
 * stream - unkeyed list and map. Unkeyed list is YANG list which did not
 * specify key.</li>
 *
 * <ul>
 * <li><code>Map</code> - Map start event is emitted using
 * {@link #startMapNode(YangInstanceIdentifier.NodeIdentifier, int)}
 * and is ended using {@link #endNode()}. Each map entry start is emitted using
 * {@link #startMapEntryNode(YangInstanceIdentifier.NodeIdentifierWithPredicates, int)}
 * with Map of keys
 * and finished using {@link #endNode()}.</li>
 *
 * <li><code>UnkeyedList</code> - Unkeyed list represent list without keys,
 * unkeyed list start is emitted using
 * {@link #startUnkeyedList(YangInstanceIdentifier.NodeIdentifier, int)} list
 * end is emitted using {@link #endNode()}. Each list item is emitted using
 * {@link #startUnkeyedListItem(YangInstanceIdentifier.NodeIdentifier, int)}
 * and ended using {@link #endNode()}.</li>
 * </ul>
 *
 * <li><code>leaf</code> - Leaf node event is emitted using
 * {@link #leafNode(YangInstanceIdentifier.NodeIdentifier, Object)}.
 * {@link #endNode()} MUST NOT BE emitted for
 * leaf node.</li>
 *
 * <li><code>leaf-list</code> - Leaf list start is emitted using
 * {@link #startLeafSet(YangInstanceIdentifier.NodeIdentifier, int)}.
 * Leaf list end is emitted using
 * {@link #endNode()}. Leaf list entries are emitted using
 * {@link #leafSetEntryNode(YangInstanceIdentifier.NodeWithValue name, Object).
 *
 * <li><code>anyxml - Anyxml node event is emitted using
 * {@link #leafNode(YangInstanceIdentifier.NodeIdentifier, Object)}. {@link #endNode()} MUST NOT BE emitted
 * for anyxml node.</code></li>
 *
 *
 * <li><code>choice</code> Choice node event is emmited by
 * {@link #startChoiceNode(YangInstanceIdentifier.NodeIdentifier, int)} event and
 * finished by invoking {@link #endNode()}
 * <li>
 * <code>augment</code> - Represents augmentation, augmentation node is started
 * by invoking {@link #startAugmentationNode(YangInstanceIdentifier.AugmentationIdentifier)} and
 * finished by invoking {@link #endNode()}.</li>
 *
 * </ul>
 *
 * <h3>Implementation notes</h3>
 *
 * <p>
 * Implementations of this interface must not hold user suppled objects
 * and resources needlessly.
 *
 */

public interface NormalizedNodeStreamWriter extends Closeable, Flushable {

    public final int UNKNOWN_SIZE = -1;

    void leafNode(YangInstanceIdentifier.NodeIdentifier name, Object value)
        throws IOException, IllegalArgumentException;

    void startLeafSet(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    void leafSetEntryNode(YangInstanceIdentifier.NodeWithValue name, Object value)
        throws IOException, IllegalArgumentException;

    void startContainerNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    void startUnkeyedList(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    void startUnkeyedListItem(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalStateException;

    void startMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    void startMapEntryNode(YangInstanceIdentifier.NodeIdentifierWithPredicates identifier, int childSizeHint)
        throws IOException, IllegalArgumentException;

    void startOrderedMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    void startChoiceNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    void startAugmentationNode(YangInstanceIdentifier.AugmentationIdentifier identifier)
        throws IOException, IllegalArgumentException;

    void anyxmlNode(YangInstanceIdentifier.NodeIdentifier name, Object value)
        throws IOException, IllegalArgumentException;

    void endNode() throws IOException, IllegalStateException;

    @Override
    void close() throws IOException;

    @Override
    void flush() throws IOException;
}
