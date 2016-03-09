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
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapNodeBuilder;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class CompositeModel {

  public static final QName TEST_QNAME = QName.create(
      "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test",
      "2014-03-13", "test");

  public static final QName AUG_QNAME = QName.create(
      "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:aug",
      "2014-03-13", "name");

  public static final QName AUG_CONTAINER = QName.create(AUG_QNAME, "aug-container");
  public static final QName AUG_INNER_CONTAINER = QName.create(AUG_QNAME, "aug-inner-container");
  public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
  public static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME,
      "outer-list");
  public static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME,
      "inner-list");
  public static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME,
      "outer-choice");
  public static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
  public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
  public static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");
  private static final String DATASTORE_TEST_YANG = "/odl-datastore-test.yang";
  private static final String DATASTORE_AUG_YANG =
      "/odl-datastore-augmentation.yang";
  private static final String DATASTORE_TEST_NOTIFICATION_YANG =
      "/odl-datastore-test-notification.yang";


  public static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier
      .of(TEST_QNAME);
  public static final YangInstanceIdentifier DESC_PATH = YangInstanceIdentifier
      .builder(TEST_PATH).node(DESC_QNAME).build();
  public static final YangInstanceIdentifier OUTER_LIST_PATH = YangInstanceIdentifier
      .builder(TEST_PATH).node(OUTER_LIST_QNAME).build();
  public static final QName TWO_QNAME = QName.create(TEST_QNAME, "two");
  public static final QName THREE_QNAME = QName.create(TEST_QNAME, "three");

  private static final Integer ONE_ID = 1;
  private static final Integer TWO_ID = 2;
  private static final String TWO_ONE_NAME = "one";
  private static final String TWO_TWO_NAME = "two";
  private static final String DESC = "Hello there";

  // Family specific constants
  public static final QName FAMILY_QNAME =
      QName
          .create(
              "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test",
              "2014-04-17", "family");
  public static final QName CHILDREN_QNAME = QName.create(FAMILY_QNAME,
      "children");
  public static final QName GRAND_CHILDREN_QNAME = QName.create(FAMILY_QNAME,
      "grand-children");
  public static final QName CHILD_NUMBER_QNAME = QName.create(FAMILY_QNAME,
      "child-number");
  public static final QName CHILD_NAME_QNAME = QName.create(FAMILY_QNAME,
      "child-name");
  public static final QName GRAND_CHILD_NUMBER_QNAME = QName.create(
      FAMILY_QNAME, "grand-child-number");
  public static final QName GRAND_CHILD_NAME_QNAME = QName.create(FAMILY_QNAME,
      "grand-child-name");

  public static final YangInstanceIdentifier FAMILY_PATH = YangInstanceIdentifier
      .of(FAMILY_QNAME);
  public static final YangInstanceIdentifier FAMILY_DESC_PATH = YangInstanceIdentifier
      .builder(FAMILY_PATH).node(DESC_QNAME).build();
  public static final YangInstanceIdentifier CHILDREN_PATH = YangInstanceIdentifier
      .builder(FAMILY_PATH).node(CHILDREN_QNAME).build();

  private static final Integer FIRST_CHILD_ID = 1;
  private static final Integer SECOND_CHILD_ID = 2;

  private static final String FIRST_CHILD_NAME = "first child";
  private static final String SECOND_CHILD_NAME = "second child";

  private static final Integer FIRST_GRAND_CHILD_ID = 1;
  private static final Integer SECOND_GRAND_CHILD_ID = 2;

  private static final String FIRST_GRAND_CHILD_NAME = "first grand child";
  private static final String SECOND_GRAND_CHILD_NAME = "second grand child";

  // first child
  private static final YangInstanceIdentifier CHILDREN_1_PATH = YangInstanceIdentifier
      .builder(CHILDREN_PATH)
      .nodeWithKey(CHILDREN_QNAME, CHILD_NUMBER_QNAME, FIRST_CHILD_ID) //
      .build();
  private static final YangInstanceIdentifier CHILDREN_1_NAME_PATH =
      YangInstanceIdentifier.builder(CHILDREN_PATH)
          .nodeWithKey(CHILDREN_QNAME, CHILD_NAME_QNAME, FIRST_CHILD_NAME) //
          .build();

  private static final YangInstanceIdentifier CHILDREN_2_PATH = YangInstanceIdentifier
      .builder(CHILDREN_PATH)
      .nodeWithKey(CHILDREN_QNAME, CHILD_NUMBER_QNAME, SECOND_CHILD_ID) //
      .build();
  private static final YangInstanceIdentifier CHILDREN_2_NAME_PATH =
      YangInstanceIdentifier.builder(CHILDREN_PATH)
          .nodeWithKey(CHILDREN_QNAME, CHILD_NAME_QNAME, SECOND_CHILD_NAME) //
          .build();


  private static final YangInstanceIdentifier GRAND_CHILD_1_PATH =
      YangInstanceIdentifier.builder(CHILDREN_1_PATH)
          .node(GRAND_CHILDREN_QNAME)
          //
          .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME,
              FIRST_GRAND_CHILD_ID) //
          .build();

  private static final YangInstanceIdentifier GRAND_CHILD_1_NAME_PATH =
      YangInstanceIdentifier.builder(CHILDREN_1_PATH)
          .node(GRAND_CHILDREN_QNAME)
          //
          .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NAME_QNAME,
              FIRST_GRAND_CHILD_NAME) //
          .build();

  private static final YangInstanceIdentifier GRAND_CHILD_2_PATH =
      YangInstanceIdentifier.builder(CHILDREN_2_PATH)
          .node(GRAND_CHILDREN_QNAME)
          //
          .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME,
              SECOND_GRAND_CHILD_ID) //
          .build();

  private static final YangInstanceIdentifier GRAND_CHILD_2_NAME_PATH =
      YangInstanceIdentifier.builder(CHILDREN_2_PATH)
          .node(GRAND_CHILDREN_QNAME)
          //
          .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NAME_QNAME,
              SECOND_GRAND_CHILD_NAME) //
          .build();

  private static final YangInstanceIdentifier DESC_PATH_ID = YangInstanceIdentifier
      .builder(DESC_PATH).build();
  private static final YangInstanceIdentifier OUTER_LIST_1_PATH =
      YangInstanceIdentifier.builder(OUTER_LIST_PATH)
          .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, ONE_ID) //
          .build();

  private static final YangInstanceIdentifier OUTER_LIST_2_PATH =
      YangInstanceIdentifier.builder(OUTER_LIST_PATH)
          .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, TWO_ID) //
          .build();

  private static final YangInstanceIdentifier TWO_TWO_PATH = YangInstanceIdentifier
      .builder(OUTER_LIST_2_PATH).node(INNER_LIST_QNAME) //
      .nodeWithKey(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME) //
      .build();

  private static final YangInstanceIdentifier TWO_TWO_VALUE_PATH =
      YangInstanceIdentifier.builder(TWO_TWO_PATH).node(VALUE_QNAME) //
          .build();

  private static final MapEntryNode BAR_NODE = mapEntryBuilder(
      OUTER_LIST_QNAME, ID_QNAME, TWO_ID) //
      .withChild(mapNodeBuilder(INNER_LIST_QNAME) //
          .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_ONE_NAME)) //
          .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME)) //
          .build()) //
      .build();

  public static final InputStream getDatastoreTestInputStream() {
    return getInputStream(DATASTORE_TEST_YANG);
  }

  public static final InputStream getDatastoreAugInputStream() {
    return getInputStream(DATASTORE_AUG_YANG);
  }

  public static final InputStream getDatastoreTestNotificationInputStream() {
    return getInputStream(DATASTORE_TEST_NOTIFICATION_YANG);
  }

  private static InputStream getInputStream(final String resourceName) {
    return TestModel.class.getResourceAsStream(resourceName);
  }

  public static SchemaContext createTestContext() {
    List<InputStream> inputStreams = new ArrayList<>();
    inputStreams.add(getDatastoreTestInputStream());
    inputStreams.add(getDatastoreAugInputStream());
    inputStreams.add(getDatastoreTestNotificationInputStream());

    YangParserImpl parser = new YangParserImpl();
    Set<Module> modules = parser.parseYangModelsFromStreams(inputStreams);
    return parser.resolveSchemaContext(modules);
  }

  /**
   * Returns a test document
   *
   * <pre>
   * test
   *     outer-list
   *          id 1
   *     outer-list
   *          id 2
   *          inner-list
   *                  name "one"
   *          inner-list
   *                  name "two"
   *
   * </pre>
   *
   * @return
   */
  public static NormalizedNode<?, ?> createDocumentOne(
      SchemaContext schemaContext) {
    return ImmutableContainerNodeBuilder
        .create()
        .withNodeIdentifier(
            new YangInstanceIdentifier.NodeIdentifier(schemaContext.getQName()))
        .withChild(createTestContainer()).build();

  }

  public static ContainerNode createTestContainer() {


    final LeafSetEntryNode<Object> nike =
        ImmutableLeafSetEntryNodeBuilder
            .create()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeWithValue<>(QName.create(TEST_QNAME,
                    "shoe"), "nike")).withValue("nike").build();
    final LeafSetEntryNode<Object> puma =
        ImmutableLeafSetEntryNodeBuilder
            .create()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeWithValue<>(QName.create(TEST_QNAME,
                    "shoe"), "puma")).withValue("puma").build();
    final LeafSetNode<Object> shoes =
        ImmutableLeafSetNodeBuilder
            .create()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(QName.create(TEST_QNAME,
                    "shoe"))).withChild(nike).withChild(puma).build();


    final LeafSetEntryNode<Object> five =
        ImmutableLeafSetEntryNodeBuilder
            .create()
            .withNodeIdentifier(
                (new YangInstanceIdentifier.NodeWithValue<>(QName.create(TEST_QNAME,
                    "number"), 5))).withValue(5).build();
    final LeafSetEntryNode<Object> fifteen =
        ImmutableLeafSetEntryNodeBuilder
            .create()
            .withNodeIdentifier(
                (new YangInstanceIdentifier.NodeWithValue<>(QName.create(TEST_QNAME,
                    "number"), 15))).withValue(15).build();
    final LeafSetNode<Object> numbers =
        ImmutableLeafSetNodeBuilder
            .create()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(QName.create(TEST_QNAME,
                    "number"))).withChild(five).withChild(fifteen).build();


    Set<QName> childAugmentations = new HashSet<>();
    childAugmentations.add(AUG_QNAME);
    final YangInstanceIdentifier.AugmentationIdentifier augmentationIdentifier =
        new YangInstanceIdentifier.AugmentationIdentifier(childAugmentations);
    final AugmentationNode augmentationNode =
        Builders.augmentationBuilder()
            .withNodeIdentifier(augmentationIdentifier)
            .withChild(ImmutableNodes.leafNode(AUG_QNAME, "First Test"))
            .build();
    return ImmutableContainerNodeBuilder
        .create()
        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME))
        .withChild(ImmutableNodes.leafNode(DESC_QNAME, DESC))
        .withChild(augmentationNode)
        .withChild(shoes)
        .withChild(numbers)
        .withChild(
            mapNodeBuilder(OUTER_LIST_QNAME)
                .withChild(mapEntry(OUTER_LIST_QNAME, ID_QNAME, ONE_ID))
                .withChild(BAR_NODE).build()).build();

  }


  public static ContainerNode createFamily() {
    final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> familyContainerBuilder =
        ImmutableContainerNodeBuilder.create().withNodeIdentifier(
            new YangInstanceIdentifier.NodeIdentifier(FAMILY_QNAME));

    final CollectionNodeBuilder<MapEntryNode, MapNode> childrenBuilder =
        mapNodeBuilder(CHILDREN_QNAME);

    final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> firstChildBuilder =
        mapEntryBuilder(CHILDREN_QNAME, CHILD_NUMBER_QNAME, FIRST_CHILD_ID);
    final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> secondChildBuilder =
        mapEntryBuilder(CHILDREN_QNAME, CHILD_NUMBER_QNAME, SECOND_CHILD_ID);

    final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> firstGrandChildBuilder =
        mapEntryBuilder(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME,
            FIRST_GRAND_CHILD_ID);
    final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> secondGrandChildBuilder =
        mapEntryBuilder(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME,
            SECOND_GRAND_CHILD_ID);

    firstGrandChildBuilder
        .withChild(
            ImmutableNodes.leafNode(GRAND_CHILD_NUMBER_QNAME,
                FIRST_GRAND_CHILD_ID)).withChild(
        ImmutableNodes.leafNode(GRAND_CHILD_NAME_QNAME,
            FIRST_GRAND_CHILD_NAME));

    secondGrandChildBuilder.withChild(
        ImmutableNodes
            .leafNode(GRAND_CHILD_NUMBER_QNAME, SECOND_GRAND_CHILD_ID))
        .withChild(
            ImmutableNodes.leafNode(GRAND_CHILD_NAME_QNAME,
                SECOND_GRAND_CHILD_NAME));

    firstChildBuilder
        .withChild(ImmutableNodes.leafNode(CHILD_NUMBER_QNAME, FIRST_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(CHILD_NAME_QNAME, FIRST_CHILD_NAME))
        .withChild(
            mapNodeBuilder(GRAND_CHILDREN_QNAME).withChild(
                firstGrandChildBuilder.build()).build());


    secondChildBuilder
        .withChild(ImmutableNodes.leafNode(CHILD_NUMBER_QNAME, SECOND_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(CHILD_NAME_QNAME, SECOND_CHILD_NAME))
        .withChild(
            mapNodeBuilder(GRAND_CHILDREN_QNAME).withChild(
                firstGrandChildBuilder.build()).build());

    childrenBuilder.withChild(firstChildBuilder.build());
    childrenBuilder.withChild(secondChildBuilder.build());

    return familyContainerBuilder.withChild(childrenBuilder.build()).build();
  }

}
