/*
 * Copyright (c) 2015 Cisco Systems and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.dsbenchmark;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.dsbenchmark.rev150105.test.exec.outer.list.InnerList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;

public final class DomListBuilder {
    // Inner List Qname identifiers for yang model's 'name' and 'value'
    private static final org.opendaylight.yangtools.yang.common.QName IL_NAME = QName.create(InnerList.QNAME, "name");
    private static final org.opendaylight.yangtools.yang.common.QName IL_VALUE = QName.create(InnerList.QNAME, "value");

    // Outer List Qname identifier for yang model's 'id'
    private static final org.opendaylight.yangtools.yang.common.QName OL_ID = QName.create(OuterList.QNAME, "id");

    public static List<MapEntryNode> buildOuterList(int outerElements, int innerElements) {
        List<MapEntryNode> outerList = new ArrayList<>(outerElements);
        for (int j = 0; j < outerElements; j++) {
            outerList.add(ImmutableNodes.mapEntryBuilder()
                                .withNodeIdentifier(new NodeIdentifierWithPredicates(OuterList.QNAME, OL_ID, j))
                                .withChild(ImmutableNodes.leafNode(OL_ID, j))
                                .withChild(buildInnerList(j, innerElements))
                                .build());
        }
        return outerList;
    }

    private static MapNode buildInnerList(int index, int elements ) {
        CollectionNodeBuilder<MapEntryNode, MapNode> innerList = ImmutableNodes.mapNodeBuilder(InnerList.QNAME);

        final String itemStr = "Item-" + String.valueOf(index) + "-";
        for (int i = 0; i < elements; i++) {
            innerList.addChild(ImmutableNodes.mapEntryBuilder()
                                .withNodeIdentifier(new NodeIdentifierWithPredicates(InnerList.QNAME, IL_NAME, i))
                                .withChild(ImmutableNodes.leafNode(IL_NAME, i))
                                .withChild(ImmutableNodes.leafNode(IL_VALUE, itemStr + String.valueOf(i)))
                                .build());
        }
        return innerList.build();
    }
}
