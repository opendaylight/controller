/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.cluster.datastore.model;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public final class TestModel {

    public static final QName TEST_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13", "test");

    public static final QName TEST2_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13", "test2");

    public static final QName JUNK_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:junk", "2014-03-13", "junk");

    public static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
    public static final QName OUTER_CONTAINER_QNAME = QName.create(TEST_QNAME, "outer-container");
    public static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
    public static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME, "outer-choice");
    public static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    public static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");
    public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
    private static final String DATASTORE_TEST_YANG = "/odl-datastore-test.yang";

    public static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_QNAME);
    public static final YangInstanceIdentifier TEST2_PATH = YangInstanceIdentifier.of(TEST2_QNAME);
    public static final YangInstanceIdentifier JUNK_PATH = YangInstanceIdentifier.of(JUNK_QNAME);
    public static final YangInstanceIdentifier OUTER_LIST_PATH = YangInstanceIdentifier.builder(TEST_PATH)
            .node(OUTER_LIST_QNAME).build();
    public static final YangInstanceIdentifier INNER_LIST_PATH = YangInstanceIdentifier.builder(TEST_PATH)
            .node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).build();
    public static final YangInstanceIdentifier OUTER_CONTAINER_PATH = TEST_PATH.node(OUTER_CONTAINER_QNAME);
    public static final QName TWO_QNAME = QName.create(TEST_QNAME,"two");
    public static final QName THREE_QNAME = QName.create(TEST_QNAME,"three");

    private TestModel() {

    }

    public static EffectiveModelContext createTestContext() {
        return YangParserTestUtils.parseYangResource(DATASTORE_TEST_YANG);
    }

    public static DataContainerChild outerMapNode() {
        return ImmutableNodes.mapNodeBuilder(OUTER_LIST_QNAME).build();
    }

    public static DataContainerChild outerNode(final int... ids) {
        CollectionNodeBuilder<MapEntryNode, SystemMapNode> outer = ImmutableNodes.mapNodeBuilder(OUTER_LIST_QNAME);
        for (int id: ids) {
            outer.addChild(ImmutableNodes.mapEntry(OUTER_LIST_QNAME, ID_QNAME, id));
        }

        return outer.build();
    }

    public static DataContainerChild outerNode(final MapEntryNode... entries) {
        CollectionNodeBuilder<MapEntryNode, SystemMapNode> outer = ImmutableNodes.mapNodeBuilder(OUTER_LIST_QNAME);
        for (MapEntryNode e: entries) {
            outer.addChild(e);
        }

        return outer.build();
    }

    public static DataContainerChild innerNode(final String... names) {
        CollectionNodeBuilder<MapEntryNode, SystemMapNode> outer = ImmutableNodes.mapNodeBuilder(INNER_LIST_QNAME);
        for (String name: names) {
            outer.addChild(ImmutableNodes.mapEntry(INNER_LIST_QNAME, NAME_QNAME, name));
        }

        return outer.build();
    }

    public static MapEntryNode outerNodeEntry(final int id, final DataContainerChild inner) {
        return ImmutableNodes.mapEntryBuilder(OUTER_LIST_QNAME, ID_QNAME, id).addChild(inner).build();
    }

    public static NormalizedNode testNodeWithOuter(final int... ids) {
        return testNodeWithOuter(outerNode(ids));
    }

    public static NormalizedNode testNodeWithOuter(final DataContainerChild outer) {
        return ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(outer).build();
    }

    public static NodeIdentifierWithPredicates outerEntryKey(final int id) {
        return NodeIdentifierWithPredicates.of(OUTER_LIST_QNAME, ID_QNAME, id);
    }

    public static YangInstanceIdentifier outerEntryPath(final int id) {
        return OUTER_LIST_PATH.node(outerEntryKey(id));
    }

    public static NodeIdentifierWithPredicates innerEntryKey(final String name) {
        return NodeIdentifierWithPredicates.of(INNER_LIST_QNAME, NAME_QNAME, name);
    }

    public static YangInstanceIdentifier innerEntryPath(final int id, final String name) {
        return OUTER_LIST_PATH.node(outerEntryKey(id)).node(INNER_LIST_QNAME).node(innerEntryKey(name));
    }

    public static YangInstanceIdentifier innerMapPath(final int id) {
        return OUTER_LIST_PATH.node(outerEntryKey(id)).node(INNER_LIST_QNAME);
    }
}
