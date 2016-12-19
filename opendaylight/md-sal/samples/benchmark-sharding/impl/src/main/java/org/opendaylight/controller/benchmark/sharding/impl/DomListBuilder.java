/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.OuterList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.clustering.sharding.simple.rev160802.test.data.outer.list.InnerList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;

public final class DomListBuilder {
    // Inner List Qname identifiers for yang model's 'name' and 'value'
    public static final org.opendaylight.yangtools.yang.common.QName IL_NAME = QName.create(InnerList.QNAME, "iid");
    public static final org.opendaylight.yangtools.yang.common.QName IL_VALUE = QName.create(InnerList.QNAME, "value");

    // Outer List Qname identifier for yang model's 'id'
    public static final org.opendaylight.yangtools.yang.common.QName OL_ID = QName.create(OuterList.QNAME, "oid");

    /**
     * Build the outer list. Not used in the sharding test
     *
     * @param outerElements Number of outer Elements
     * @param innerElements Number of Inner elements
     * @return Normalized node corresponding to the outer list
     */
    public static List<MapEntryNode> buildOuterList(final int outerElements, final long innerElements) {
        List<MapEntryNode> outerList = new ArrayList<>(outerElements);
        for (long j = 0; j < outerElements; j++) {
            outerList.add(ImmutableNodes.mapEntryBuilder()
                                .withNodeIdentifier(new NodeIdentifierWithPredicates(OuterList.QNAME, OL_ID, j))
                                .withChild(ImmutableNodes.leafNode(OL_ID, j))
                                .withChild(buildInnerList(j, innerElements))
                                .build());
        }
        return outerList;
    }

    /**
     * Build the inner list (the per-shard data).
     * @param i2 outer list item index (to recognize which outer list these
     *            inner list elements belong to)
     * @param numItems number if items in the list
     * @return Normalized node corresponding to the inner list
     */
    public static MapNode buildInnerList(final long i2, final long numItems) {
        CollectionNodeBuilder<MapEntryNode, MapNode> innerList = ImmutableNodes.mapNodeBuilder(InnerList.QNAME);

        final String itemStr = "Item-" + String.valueOf(i2) + "-";
        for (long i = 0; i < numItems; i++) {
            innerList.addChild(ImmutableNodes.mapEntryBuilder()
                                .withNodeIdentifier(new NodeIdentifierWithPredicates(InnerList.QNAME, IL_NAME, i))
                                .withChild(ImmutableNodes.leafNode(IL_NAME, i))
                                .withChild(ImmutableNodes.leafNode(IL_VALUE, itemStr + String.valueOf(i)))
                                .build());
        }
        return innerList.build();
    }
}
