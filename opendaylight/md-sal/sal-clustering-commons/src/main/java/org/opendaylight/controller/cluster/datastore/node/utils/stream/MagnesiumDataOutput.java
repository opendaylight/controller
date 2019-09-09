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
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

final class MagnesiumDataOutput extends AbstractNormalizedNodeDataOutput {
    MagnesiumDataOutput(final DataOutput output) {
        super(output);
    }

    @Override
    public void startLeafNode(final NodeIdentifier name) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startOrderedLeafSet(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startLeafSetEntryNode(final NodeWithValue<?> name) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startContainerNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startUnkeyedList(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startUnkeyedListItem(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startMapEntryNode(final NodeIdentifierWithPredicates identifier, final int childSizeHint)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startOrderedMapNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startChoiceNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startAugmentationNode(final AugmentationIdentifier identifier) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startAnyxmlNode(final NodeIdentifier name) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void domSourceValue(final DOMSource value) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startYangModeledAnyXmlNode(final NodeIdentifier name, final int childSizeHint) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void endNode() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void scalarValue(final Object value) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    short streamVersion() {
        return TokenTypes.MAGNESIUM_VERSION;
    }

    @Override
    void writeQNameInternal(final QName qname) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void writePathArgumentInternal(final PathArgument pathArgument) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void writeYangInstanceIdentifierInternal(final YangInstanceIdentifier identifier) throws IOException {
        // TODO Auto-generated method stub

    }
}
