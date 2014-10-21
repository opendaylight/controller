
/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

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

    /**
     * Write the leaf node identifier and value to the stream.
     * @param name
     * @param value
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void leafNode(YangInstanceIdentifier.NodeIdentifier name, Object value)
        throws IOException, IllegalArgumentException;

    /**
     * Start writing leaf Set node. You must call {@link #endNode()} once you are done writing all of its children.
     * @param name
     * @param childSizeHint is the estimated children count. Usage is optional in implementation.
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void startLeafSet(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    /**
     * Write the leaf Set Entry Node object to the stream with identifier and value.
     * @param name
     * @param value
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void leafSetEntryNode(YangInstanceIdentifier.NodeWithValue name, Object value)
        throws IOException, IllegalArgumentException;

    /**
     * Start writing container node. You must call {@link #endNode()} once you are done writing all of its children.
     * @param name
     * @param childSizeHint is the estimated children count. Usage is optional in implementation.
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void startContainerNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    /**
     * Start writing unkeyed list node. You must call {@link #endNode()} once you are done writing all of its children.
     * @param name
     * @param childSizeHint is the estimated children count. Usage is optional in implementation.
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void startUnkeyedList(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    /**
     * Start writing unkeyed list item. You must call {@link #endNode()} once you are done writing all of its children.
     * @param name
     * @param childSizeHint is the estimated children count. Usage is optional in implementation.
     * @throws IOException
     * @throws IllegalStateException
     */
    void startUnkeyedListItem(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalStateException;

    /**
     * Start writing map node. You must call {@link #endNode()} once you are done writing all of its children.
     * @param name
     * @param childSizeHint is the estimated children count. Usage is optional in implementation.
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void startMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    /**
     * Start writing map entry node. You must call {@link #endNode()} once you are done writing all of its children.
     * @param identifier
     * @param childSizeHint is the estimated children count. Usage is optional in implementation.
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void startMapEntryNode(YangInstanceIdentifier.NodeIdentifierWithPredicates identifier, int childSizeHint)
        throws IOException, IllegalArgumentException;

    /**
     * Start writing ordered map node. You must call {@link #endNode()} once you are done writing all of its children.
     * @param name
     * @param childSizeHint is the estimated children count. Usage is optional in implementation.
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void startOrderedMapNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    /**
     * Start writing choice node. You must call {@link #endNode()} once you are done writing all of its children.
     * @param name
     * @param childSizeHint is the estimated children count. Usage is optional in implementation.
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void startChoiceNode(YangInstanceIdentifier.NodeIdentifier name, int childSizeHint)
        throws IOException, IllegalArgumentException;

    /**
     * Start writing augmentation node. You must call {@link #endNode()} once you are done writing all of its children.
     * @param identifier
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void startAugmentationNode(YangInstanceIdentifier.AugmentationIdentifier identifier)
        throws IOException, IllegalArgumentException;

    /**
     * Write any xml node identifier and value to the stream
     * @param name
     * @param value
     * @throws IOException
     * @throws IllegalArgumentException
     */
    void anyxmlNode(YangInstanceIdentifier.NodeIdentifier name, Object value)
        throws IOException, IllegalArgumentException;

    /**
     * This method should be used to add end symbol/identifier of node in the stream.
     * @throws IOException
     * @throws IllegalStateException
     */
    void endNode() throws IOException, IllegalStateException;

    @Override
    void close() throws IOException;

    @Override
    void flush() throws IOException;
}
