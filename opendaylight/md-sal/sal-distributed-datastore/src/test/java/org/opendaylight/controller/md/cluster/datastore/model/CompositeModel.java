/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.cluster.datastore.model;

import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntry;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntryBuilder;
import static org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes.leafNode;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public final class CompositeModel {
    public static final QName TEST_QNAME = QName.create(
        "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13", "test");

    public static final QName AUG_QNAME = QName.create(
        "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:aug", "2014-03-13", "name");

    public static final QName AUG_CONTAINER = QName.create(AUG_QNAME, "aug-container");
    public static final QName AUG_INNER_CONTAINER = QName.create(AUG_QNAME, "aug-inner-container");
    public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
    public static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
    public static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
    public static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME, "outer-choice");
    public static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    public static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");
    private static final String DATASTORE_TEST_YANG = "/odl-datastore-test.yang";
    private static final String DATASTORE_AUG_YANG = "/odl-datastore-augmentation.yang";
    private static final String DATASTORE_TEST_NOTIFICATION_YANG = "/odl-datastore-test-notification.yang";

    public static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_QNAME);
    public static final YangInstanceIdentifier DESC_PATH = YangInstanceIdentifier.builder(TEST_PATH).node(DESC_QNAME)
            .build();
    public static final YangInstanceIdentifier OUTER_LIST_PATH = YangInstanceIdentifier.builder(TEST_PATH)
            .node(OUTER_LIST_QNAME).build();
    public static final QName TWO_QNAME = QName.create(TEST_QNAME, "two");
    public static final QName THREE_QNAME = QName.create(TEST_QNAME, "three");

    private static final Integer ONE_ID = 1;
    private static final Integer TWO_ID = 2;
    private static final String TWO_ONE_NAME = "one";
    private static final String TWO_TWO_NAME = "two";
    private static final String DESC = "Hello there";

    // Family specific constants
    public static final QName FAMILY_QNAME = QName.create(
            "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test", "2014-04-17",
            "family");
    public static final QName CHILDREN_QNAME = QName.create(FAMILY_QNAME, "children");
    public static final QName GRAND_CHILDREN_QNAME = QName.create(FAMILY_QNAME, "grand-children");
    public static final QName CHILD_NUMBER_QNAME = QName.create(FAMILY_QNAME, "child-number");
    public static final QName CHILD_NAME_QNAME = QName.create(FAMILY_QNAME, "child-name");
    public static final QName GRAND_CHILD_NUMBER_QNAME = QName.create(FAMILY_QNAME, "grand-child-number");
    public static final QName GRAND_CHILD_NAME_QNAME = QName.create(FAMILY_QNAME, "grand-child-name");

    public static final YangInstanceIdentifier FAMILY_PATH = YangInstanceIdentifier.of(FAMILY_QNAME);
    public static final YangInstanceIdentifier FAMILY_DESC_PATH = YangInstanceIdentifier.builder(FAMILY_PATH)
            .node(DESC_QNAME).build();
    public static final YangInstanceIdentifier CHILDREN_PATH = YangInstanceIdentifier.builder(FAMILY_PATH)
            .node(CHILDREN_QNAME).build();

    private static final Integer FIRST_CHILD_ID = 1;
    private static final Integer SECOND_CHILD_ID = 2;

    private static final String FIRST_CHILD_NAME = "first child";
    private static final String SECOND_CHILD_NAME = "second child";

    private static final Integer FIRST_GRAND_CHILD_ID = 1;
    private static final Integer SECOND_GRAND_CHILD_ID = 2;

    private static final String FIRST_GRAND_CHILD_NAME = "first grand child";
    private static final String SECOND_GRAND_CHILD_NAME = "second grand child";

    private static final MapEntryNode BAR_NODE = mapEntryBuilder(OUTER_LIST_QNAME, ID_QNAME, TWO_ID)
        .withChild(ImmutableNodes.newSystemMapBuilder()
            .withNodeIdentifier(new NodeIdentifier(INNER_LIST_QNAME))
            .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_ONE_NAME))
            .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME))
            .build())
        .build();

    private CompositeModel() {
        // Hidden on purpose
    }

    public static SchemaContext createTestContext() {
        return YangParserTestUtils.parseYangResources(CompositeModel.class, DATASTORE_TEST_YANG, DATASTORE_AUG_YANG,
            DATASTORE_TEST_NOTIFICATION_YANG);
    }

    public static ContainerNode createTestContainer() {
        return ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            .withChild(leafNode(DESC_QNAME, DESC))
            .withChild(leafNode(AUG_QNAME, "First Test"))
            .withChild(ImmutableNodes.<String>newSystemLeafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create(TEST_QNAME, "shoe")))
                .withChildValue("nike")
                .withChildValue("puma")
                .build())
            .withChild(ImmutableNodes.<Integer>newSystemLeafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create(TEST_QNAME, "number")))
                .withChildValue(5)
                .withChildValue(15)
                .build())
            .withChild(ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(new NodeIdentifier(OUTER_LIST_QNAME))
                .withChild(mapEntry(OUTER_LIST_QNAME, ID_QNAME, ONE_ID))
                .withChild(BAR_NODE)
                .build())
            .build();
    }

    public static ContainerNode createFamily() {
        final var grandChildren = ImmutableNodes.newSystemMapBuilder()
            .withNodeIdentifier(new NodeIdentifier(GRAND_CHILDREN_QNAME))
            .withChild(mapEntryBuilder(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID)
                .withChild(leafNode(GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID))
                .withChild(leafNode(GRAND_CHILD_NAME_QNAME, FIRST_GRAND_CHILD_NAME))
                .build())
            .build();

        return ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(FAMILY_QNAME))
            .withChild(ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(new NodeIdentifier(CHILDREN_QNAME))
                .withChild(mapEntryBuilder(CHILDREN_QNAME, CHILD_NUMBER_QNAME, FIRST_CHILD_ID)
                    .withChild(leafNode(CHILD_NUMBER_QNAME, FIRST_CHILD_ID))
                    .withChild(leafNode(CHILD_NAME_QNAME, FIRST_CHILD_NAME))
                    .withChild(grandChildren)
                    .build())
                .withChild(mapEntryBuilder(CHILDREN_QNAME, CHILD_NUMBER_QNAME, SECOND_CHILD_ID)
                    .withChild(leafNode(CHILD_NUMBER_QNAME, SECOND_CHILD_ID))
                    .withChild(leafNode(CHILD_NAME_QNAME, SECOND_CHILD_NAME))
                    .withChild(grandChildren)
                    .build())
                .build())
            .build();
    }
}
