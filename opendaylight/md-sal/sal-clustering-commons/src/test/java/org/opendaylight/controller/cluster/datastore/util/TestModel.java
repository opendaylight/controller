/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.util;

import static org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes.leafNode;

import com.google.common.collect.ImmutableSet;
import java.io.InputStream;
import java.util.List;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public final class TestModel {

    public static final QName TEST_QNAME = QName.create(
            "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13", "test");

    public static final QName AUG_NAME_QNAME = QName.create(
            "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:aug", "2014-03-13", "name");

    public static final QName AUG_CONT_QNAME = QName.create(
            "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:aug", "2014-03-13", "cont");


    public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
    public static final QName POINTER_QNAME = QName.create(TEST_QNAME, "pointer");
    public static final QName SOME_BINARY_DATA_QNAME = QName.create(TEST_QNAME, "some-binary-data");
    public static final QName BINARY_LEAF_LIST_QNAME = QName.create(TEST_QNAME, "binary_leaf_list");
    public static final QName SOME_REF_QNAME = QName.create(TEST_QNAME, "some-ref");
    public static final QName MYIDENTITY_QNAME = QName.create(TEST_QNAME, "myidentity");
    public static final QName SWITCH_FEATURES_QNAME = QName.create(TEST_QNAME, "switch-features");

    public static final QName AUGMENTED_LIST_QNAME = QName.create(TEST_QNAME, "augmented-list");
    public static final QName AUGMENTED_LIST_ENTRY_QNAME = QName.create(TEST_QNAME, "augmented-list-entry");

    public static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
    public static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
    public static final QName INNER_CONTAINER_QNAME = QName.create(TEST_QNAME, "inner-container");
    public static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME, "outer-choice");
    public static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    public static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");
    public static final QName BOOLEAN_LEAF_QNAME = QName.create(TEST_QNAME, "boolean-leaf");
    public static final QName SHORT_LEAF_QNAME = QName.create(TEST_QNAME, "short-leaf");
    public static final QName BYTE_LEAF_QNAME = QName.create(TEST_QNAME, "byte-leaf");
    public static final QName BIGINTEGER_LEAF_QNAME = QName.create(TEST_QNAME, "biginteger-leaf");
    public static final QName BIGDECIMAL_LEAF_QNAME = QName.create(TEST_QNAME, "bigdecimal-leaf");
    public static final QName ORDERED_LIST_QNAME = QName.create(TEST_QNAME, "ordered-list");
    public static final QName ORDERED_LIST_ENTRY_QNAME = QName.create(TEST_QNAME, "ordered-list-leaf");
    public static final QName UNKEYED_LIST_QNAME = QName.create(TEST_QNAME, "unkeyed-list");
    public static final QName UNKEYED_LIST_ENTRY_QNAME = QName.create(TEST_QNAME, "unkeyed-list-entry");
    public static final QName CHOICE_QNAME = QName.create(TEST_QNAME, "choice");
    public static final QName SHOE_QNAME = QName.create(TEST_QNAME, "shoe");
    public static final QName ANY_XML_QNAME = QName.create(TEST_QNAME, "any");
    public static final QName EMPTY_QNAME = QName.create(TEST_QNAME, "empty-leaf");
    public static final QName INVALID_QNAME = QName.create(TEST_QNAME, "invalid");
    private static final String DATASTORE_TEST_YANG = "/odl-datastore-test.yang";
    private static final String DATASTORE_AUG_YANG = "/odl-datastore-augmentation.yang";
    private static final String DATASTORE_TEST_NOTIFICATION_YANG = "/odl-datastore-test-notification.yang";

    public static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_QNAME);
    public static final YangInstanceIdentifier DESC_PATH = YangInstanceIdentifier
            .builder(TEST_PATH).node(DESC_QNAME).build();
    public static final YangInstanceIdentifier OUTER_LIST_PATH =
            YangInstanceIdentifier.builder(TEST_PATH).node(OUTER_LIST_QNAME).build();
    public static final QName TWO_THREE_QNAME = QName.create(TEST_QNAME, "two");
    public static final QName TWO_QNAME = QName.create(TEST_QNAME, "two");
    public static final QName THREE_QNAME = QName.create(TEST_QNAME, "three");

    private static final Integer ONE_ID = 1;
    private static final Integer TWO_ID = 2;
    private static final String TWO_ONE_NAME = "one";
    private static final String TWO_TWO_NAME = "two";
    private static final String DESC = "Hello there";
    private static final Boolean ENABLED = true;
    private static final Short SHORT_ID = 1;
    private static final Byte BYTE_ID = 1;
    // Family specific constants
    public static final QName FAMILY_QNAME = QName.create(
        "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test", "2014-04-17", "family");
    public static final QName CHILDREN_QNAME = QName.create(FAMILY_QNAME, "children");
    public static final QName GRAND_CHILDREN_QNAME = QName.create(FAMILY_QNAME, "grand-children");
    public static final QName CHILD_NUMBER_QNAME = QName.create(FAMILY_QNAME, "child-number");
    public static final QName CHILD_NAME_QNAME = QName.create(FAMILY_QNAME, "child-name");
    public static final QName GRAND_CHILD_NUMBER_QNAME = QName.create(FAMILY_QNAME, "grand-child-number");
    public static final QName GRAND_CHILD_NAME_QNAME = QName.create(FAMILY_QNAME,"grand-child-name");

    public static final YangInstanceIdentifier FAMILY_PATH = YangInstanceIdentifier.of(FAMILY_QNAME);
    public static final YangInstanceIdentifier FAMILY_DESC_PATH =
            YangInstanceIdentifier.builder(FAMILY_PATH).node(DESC_QNAME).build();
    public static final YangInstanceIdentifier CHILDREN_PATH =
            YangInstanceIdentifier.builder(FAMILY_PATH).node(CHILDREN_QNAME).build();

    private static final Integer FIRST_CHILD_ID = 1;
    private static final Integer SECOND_CHILD_ID = 2;

    private static final String FIRST_CHILD_NAME = "first child";
    private static final String SECOND_CHILD_NAME = "second child";

    private static final Integer FIRST_GRAND_CHILD_ID = 1;
    private static final Integer SECOND_GRAND_CHILD_ID = 2;

    private static final String FIRST_GRAND_CHILD_NAME = "first grand child";
    private static final String SECOND_GRAND_CHILD_NAME = "second grand child";

    private static final MapEntryNode BAR_NODE = ImmutableNodes.newMapEntryBuilder()
        .withNodeIdentifier(NodeIdentifierWithPredicates.of(OUTER_LIST_QNAME, ID_QNAME, TWO_ID))
        .withChild(leafNode(ID_QNAME, TWO_ID))
        .withChild(ImmutableNodes.newSystemMapBuilder()
            .withNodeIdentifier(new NodeIdentifier(INNER_LIST_QNAME))
            .withChild(ImmutableNodes.newMapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(INNER_LIST_QNAME, NAME_QNAME, TWO_ONE_NAME))
                .withChild(leafNode(NAME_QNAME, TWO_ONE_NAME))
                .build())
            .withChild(ImmutableNodes.newMapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME))
                .withChild(leafNode(NAME_QNAME, TWO_TWO_NAME))
                .build())
            .build())
        .build();

    private TestModel() {
        throw new UnsupportedOperationException();
    }

    public static InputStream getDatastoreTestInputStream() {
        return getInputStream(DATASTORE_TEST_YANG);
    }

    public static InputStream getDatastoreAugInputStream() {
        return getInputStream(DATASTORE_AUG_YANG);
    }

    public static InputStream getDatastoreTestNotificationInputStream() {
        return getInputStream(DATASTORE_TEST_NOTIFICATION_YANG);
    }

    private static InputStream getInputStream(final String resourceName) {
        return TestModel.class.getResourceAsStream(resourceName);
    }

    public static EffectiveModelContext createTestContext() {
        return YangParserTestUtils.parseYangResources(TestModel.class, DATASTORE_TEST_YANG, DATASTORE_AUG_YANG,
            DATASTORE_TEST_NOTIFICATION_YANG);
    }

    public static EffectiveModelContext createTestContextWithoutTestSchema() {
        return YangParserTestUtils.parseYangResource(DATASTORE_TEST_NOTIFICATION_YANG);
    }

    public static EffectiveModelContext createTestContextWithoutAugmentationSchema() {
        return YangParserTestUtils.parseYangResources(TestModel.class, DATASTORE_TEST_YANG,
            DATASTORE_TEST_NOTIFICATION_YANG);
    }

    public static DataContainerNodeBuilder<NodeIdentifier, ContainerNode> createBaseTestContainerBuilder() {
        // Create YangInstanceIdentifier with all path arg types.
        YangInstanceIdentifier instanceID = YangInstanceIdentifier.of(
            new NodeIdentifier(QName.create(TEST_QNAME, "qname")),
            NodeIdentifierWithPredicates.of(QName.create(TEST_QNAME, "list-entry"),
                QName.create(TEST_QNAME, "key"), 10),
            new NodeWithValue<>(QName.create(TEST_QNAME, "leaf-list-entry"), "foo"));

        // Create the document
        return ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TEST_QNAME))
            // Create a bits leaf
            .withChild(leafNode(QName.create(TEST_QNAME, "my-bits"), ImmutableSet.of("foo", "bar")))
            .withChild(leafNode(DESC_QNAME, DESC))
            .withChild(leafNode(BOOLEAN_LEAF_QNAME, ENABLED))
            .withChild(leafNode(SHORT_LEAF_QNAME, SHORT_ID))
            .withChild(leafNode(BYTE_LEAF_QNAME, BYTE_ID))
            .withChild(leafNode(TestModel.BIGINTEGER_LEAF_QNAME, Uint64.valueOf(100)))
            .withChild(leafNode(TestModel.BIGDECIMAL_LEAF_QNAME, Decimal64.valueOf("1.2").scaleTo(2)))
            .withChild(leafNode(SOME_REF_QNAME, instanceID))
            .withChild(leafNode(MYIDENTITY_QNAME, DESC_QNAME))
            .withChild(ImmutableNodes.newUnkeyedListBuilder()
                .withNodeIdentifier(new NodeIdentifier(UNKEYED_LIST_QNAME))
                // Create unkeyed list entry
                .withChild(ImmutableNodes.newUnkeyedListEntryBuilder()
                    .withNodeIdentifier(new NodeIdentifier(UNKEYED_LIST_QNAME))
                    .withChild(leafNode(NAME_QNAME, "unkeyed-entry-name"))
                    .build())
                .build())
            .withChild(ImmutableNodes.newChoiceBuilder()
                .withNodeIdentifier(new NodeIdentifier(TWO_THREE_QNAME))
                .withChild(leafNode(TWO_QNAME, "two")).build())
            .withChild(ImmutableNodes.newUserMapBuilder()
                .withNodeIdentifier(new NodeIdentifier(ORDERED_LIST_QNAME))
                .withValue(List.of(
                    ImmutableNodes.newMapEntryBuilder()
                        .withNodeIdentifier(NodeIdentifierWithPredicates.of(ORDERED_LIST_QNAME,
                            ORDERED_LIST_ENTRY_QNAME, "1"))
                        .withChild(leafNode(ORDERED_LIST_ENTRY_QNAME, "1"))
                        .build(),
                    ImmutableNodes.newMapEntryBuilder()
                        .withNodeIdentifier(NodeIdentifierWithPredicates.of(ORDERED_LIST_QNAME,
                            ORDERED_LIST_ENTRY_QNAME, "2"))
                        .withChild(leafNode(ORDERED_LIST_ENTRY_QNAME, "2"))
                    .build()))
                .build())
            .withChild(ImmutableNodes.newSystemLeafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(SHOE_QNAME))
                .withChildValue("nike")
                .withChildValue("puma")
                .build())
            .withChild(ImmutableNodes.newSystemLeafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(QName.create(TEST_QNAME, "number")))
                .withChildValue(5)
                .withChildValue(15)
                .build())
            .withChild(ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new NodeIdentifier(SWITCH_FEATURES_QNAME))
                // Test a leaf-list where each entry contains an identity
                .withChild(ImmutableNodes.newSystemLeafSetBuilder()
                    .withNodeIdentifier(new NodeIdentifier(QName.create(TEST_QNAME, "capability")))
                    .withChildValue(DESC_QNAME)
                    .build())
                .build())
            .withChild(ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(new NodeIdentifier(AUGMENTED_LIST_QNAME))
                // Create augmentations
                .withChild(createAugmentedListEntry(1, "First Test"))
                .build())
            .withChild(ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(new NodeIdentifier(OUTER_LIST_QNAME))
                .withChild(ImmutableNodes.newMapEntryBuilder()
                    .withNodeIdentifier(NodeIdentifierWithPredicates.of(OUTER_LIST_QNAME, ID_QNAME, ONE_ID))
                    .withChild(leafNode(ID_QNAME, ONE_ID))
                    .build())
                .withChild(BAR_NODE)
                .build());
    }

    public static ContainerNode createTestContainer() {
        return createBaseTestContainerBuilder().build();
    }

    public static MapEntryNode createAugmentedListEntry(final int id, final String name) {
        return ImmutableNodes.newMapEntryBuilder()
            .withNodeIdentifier(NodeIdentifierWithPredicates.of(AUGMENTED_LIST_QNAME, ID_QNAME, id))
            .withChild(leafNode(ID_QNAME, id))
            .withChild(ImmutableNodes.newContainerBuilder()
                .withNodeIdentifier(new NodeIdentifier(AUG_CONT_QNAME))
                .withChild(leafNode(AUG_NAME_QNAME, name))
                .build())
            .build();
    }

    public static ContainerNode createFamily() {
        final var firstGrandChildBuilder = ImmutableNodes.newMapEntryBuilder()
            .withNodeIdentifier(NodeIdentifierWithPredicates.of(GRAND_CHILDREN_QNAME,
                GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID))
            .withChild(leafNode(GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID))
            .withChild(leafNode(GRAND_CHILD_NAME_QNAME, FIRST_GRAND_CHILD_NAME));

        return ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(FAMILY_QNAME))
            .withChild(ImmutableNodes.newSystemMapBuilder()
                .withNodeIdentifier(new NodeIdentifier(CHILDREN_QNAME))
                .withChild(ImmutableNodes.newMapEntryBuilder()
                    .withNodeIdentifier(NodeIdentifierWithPredicates.of(CHILDREN_QNAME,
                        CHILD_NUMBER_QNAME, FIRST_CHILD_ID))
                    .withChild(leafNode(CHILD_NUMBER_QNAME, FIRST_CHILD_ID))
                    .withChild(leafNode(CHILD_NAME_QNAME, FIRST_CHILD_NAME))
                    .withChild(ImmutableNodes.newSystemMapBuilder()
                        .withNodeIdentifier(new NodeIdentifier(GRAND_CHILDREN_QNAME))
                        .withChild(firstGrandChildBuilder.build())
                        .build())
                    .build())
                .withChild(ImmutableNodes.newMapEntryBuilder()
                    .withNodeIdentifier(NodeIdentifierWithPredicates.of(CHILDREN_QNAME,
                        CHILD_NUMBER_QNAME, SECOND_CHILD_ID))
                    .withChild(leafNode(CHILD_NUMBER_QNAME, SECOND_CHILD_ID))
                    .withChild(leafNode(CHILD_NAME_QNAME, SECOND_CHILD_NAME))
                    .withChild(ImmutableNodes.newSystemMapBuilder()
                        .withNodeIdentifier(new NodeIdentifier(GRAND_CHILDREN_QNAME))
                        .withChild(firstGrandChildBuilder.build())
                        .build())
                    .build())
                .build())
            .build();
    }
}
