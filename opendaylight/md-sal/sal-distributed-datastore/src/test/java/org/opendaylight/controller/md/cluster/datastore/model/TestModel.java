/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.cluster.datastore.model;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.SystemMapNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
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

    public static final @NonNull ContainerNode EMPTY_TEST = ImmutableNodes.newContainerBuilder()
        .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
        .build();
    public static final @NonNull ContainerNode EMPTY_TEST2 = ImmutableNodes.newContainerBuilder()
        .withNodeIdentifier(new NodeIdentifier(TEST2_QNAME))
        .build();
    public static final @NonNull SystemMapNode EMPTY_OUTER_LIST = ImmutableNodes.newSystemMapBuilder()
        .withNodeIdentifier(new NodeIdentifier(OUTER_LIST_QNAME))
        .build();

    private TestModel() {
        // Hidden on purpose
    }

    public static EffectiveModelContext createTestContext() {
        return YangParserTestUtils.parseYangResource(DATASTORE_TEST_YANG);
    }

    public static SystemMapNode outerNode(final int... ids) {
        var outer = ImmutableNodes.newSystemMapBuilder().withNodeIdentifier(new NodeIdentifier(OUTER_LIST_QNAME));
        for (int id: ids) {
            outer.addChild(ImmutableNodes.newMapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(OUTER_LIST_QNAME, ID_QNAME, id))
                .withChild(ImmutableNodes.leafNode(ID_QNAME, id))
                .build());
        }

        return outer.build();
    }

    public static SystemMapNode outerNode(final MapEntryNode... entries) {
        var outer = ImmutableNodes.newSystemMapBuilder().withNodeIdentifier(new NodeIdentifier(OUTER_LIST_QNAME));
        for (var entry : entries) {
            outer.addChild(entry);
        }

        return outer.build();
    }

    public static SystemMapNode innerNode(final String... names) {
        var outer = ImmutableNodes.newSystemMapBuilder().withNodeIdentifier(new NodeIdentifier(INNER_LIST_QNAME));
        for (var name : names) {
            outer.addChild(ImmutableNodes.newMapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(INNER_LIST_QNAME, NAME_QNAME, name))
                .withChild(ImmutableNodes.leafNode(NAME_QNAME, name))
                .build());
        }
        return outer.build();
    }

    public static MapEntryNode outerEntry(final int id, final DataContainerChild... children) {
        final var builder = ImmutableNodes.newMapEntryBuilder()
            .withNodeIdentifier(outerEntryKey(id))
            .withChild(ImmutableNodes.leafNode(ID_QNAME, id));

        for (var child : children) {
            builder.addChild(child);
        }

        return builder.build();
    }

    public static ContainerNode testNodeWithOuter(final int... ids) {
        return testNodeWithOuter(outerNode(ids));
    }

    public static ContainerNode testNodeWithOuter(final DataContainerChild outer) {
        return ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            .withChild(outer)
            .build();
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
