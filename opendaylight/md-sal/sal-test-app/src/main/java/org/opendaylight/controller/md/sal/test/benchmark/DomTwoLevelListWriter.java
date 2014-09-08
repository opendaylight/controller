/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.test.benchmark;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.DataOperation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.base.rev140701.TwoLevelListProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;

class DomTwoLevelListWriter extends AbstractTwoLevelListWriter<YangInstanceIdentifier, NormalizedNode<?,?>, OrderedMapNode> {

    private static final QName NAME_QNAME = QName.create(NestedList.QNAME, "name");
    private static final QName TYPE_QNAME = QName.create(NestedList.QNAME, "type");

    public DomTwoLevelListWriter(TwoLevelListProperties input, DataOperation writeOperation) {
        super(input,TransactionWriter.DomTransactionWriter.from(writeOperation));
    }

    @Override
    OrderedMapNode createInnerList(long innerItems) {
        CollectionNodeBuilder<MapEntryNode, OrderedMapNode> map = Builders.orderedMapBuilder();
        map.withNodeIdentifier(new NodeIdentifier(NestedList.QNAME));
        for(int i =0;i<innerItems;i++) {
            String key = String.valueOf(i);
            NodeIdentifierWithPredicates identifier = new NodeIdentifierWithPredicates(NestedList.QNAME, NAME_QNAME,key);
            map.addChild(Builders.mapEntryBuilder()
                    .withNodeIdentifier(identifier)
                    .addChild(ImmutableNodes.leafNode(NAME_QNAME, key))
                    .addChild(ImmutableNodes.leafNode(TYPE_QNAME, "dom"))
                    .build());

        }
        return map.build();
    }

    @Override
    NormalizedNode<?, ?> createOuterListItem(long id, OrderedMapNode innerList) {
        final String key = String.valueOf(id);
        final NodeIdentifierWithPredicates identifier = new NodeIdentifierWithPredicates(TopLevelList.QNAME, NAME_QNAME,key);
        return Builders.mapEntryBuilder()
        .withNodeIdentifier(identifier)
        .addChild(ImmutableNodes.leafNode(NAME_QNAME,key))
        .addChild(innerList)
        .build();
    }

    @Override
    YangInstanceIdentifier createOuterListItemPath(long id) {
        final String key = String.valueOf(id);
        return YangInstanceIdentifier.builder()
                .node(Top.QNAME)
                .node(TopLevelList.QNAME)
                .nodeWithKey(TopLevelList.QNAME, NAME_QNAME,key)
                .build();
    }

}