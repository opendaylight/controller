/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import javax.xml.transform.dom.DOMSource;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;

final class DummyNormalizedNodeStreamWriter implements NormalizedNodeStreamWriter {
    static final DummyNormalizedNodeStreamWriter INSTANCE = new DummyNormalizedNodeStreamWriter();

    private DummyNormalizedNodeStreamWriter() {
        // Hidden
    }

    @Override
    public void startLeafNode(final NodeIdentifier name) {
        // No-op
    }

    @Override
    public void startLeafSet(final NodeIdentifier name, final int childSizeHint) {
        // No-op
    }

    @Override
    public void startOrderedLeafSet(final NodeIdentifier name, final int childSizeHint) {
        // No-op
    }

    @Override
    public void startLeafSetEntryNode(final NodeWithValue<?> name) {
        // No-op
    }

    @Override
    public void startContainerNode(final NodeIdentifier name, final int childSizeHint) {
        // No-op
    }

    @Override
    public void startUnkeyedList(final NodeIdentifier name, final int childSizeHint) {
        // No-op
    }

    @Override
    public void startUnkeyedListItem(final NodeIdentifier name, final int childSizeHint) {
        // No-op
    }

    @Override
    public void startMapNode(final NodeIdentifier name, final int childSizeHint) {
        // No-op
    }

    @Override
    public void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint) {
        // No-op
    }

    @Override
    public void startOrderedMapNode(final NodeIdentifier name, final int childSizeHint) {
        // No-op
    }

    @Override
    public void startChoiceNode(final NodeIdentifier name, final int childSizeHint) {
        // No-op
    }

    @Override
    public void startAugmentationNode(final AugmentationIdentifier identifier) {
        // No-op
    }

    @Override
    public void startAnyxmlNode(final NodeIdentifier name) {
        // No-op
    }

    @Override
    public void domSourceValue(final DOMSource value) {
        // No-op
    }

    @Override
    public void startYangModeledAnyXmlNode(final NodeIdentifier name, final int childSizeHint) {
        // No-op
    }

    @Override
    public void endNode() {
        // No-op
    }

    @Override
    public void scalarValue(@NonNull final Object value) {
        // No-op
    }

    @Override
    public void close() {
        // No-op
    }

    @Override
    public void flush() {
        // No-op
    }
}
