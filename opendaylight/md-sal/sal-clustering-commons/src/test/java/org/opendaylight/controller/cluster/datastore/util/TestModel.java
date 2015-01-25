/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.util;

import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntry;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntryBuilder;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapNodeBuilder;
import com.google.common.collect.ImmutableSet;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapEntryNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class TestModel {

  public static final QName TEST_QNAME = QName.create(
      "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test",
      "2014-03-13", "test");

  public static final QName AUG_NAME_QNAME = QName.create(
      "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:aug",
      "2014-03-13", "name");

  public static final QName AUG_CONT_QNAME = QName.create(
      "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:aug",
      "2014-03-13", "cont");


  public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
  public static final QName POINTER_QNAME = QName.create(TEST_QNAME, "pointer");
  public static final QName SOME_BINARY_DATE_QNAME = QName.create(TEST_QNAME, "some-binary-data");
  public static final QName BINARY_LEAF_LIST_QNAME = QName.create(TEST_QNAME, "binary_leaf_list");
  public static final QName SOME_REF_QNAME = QName.create(TEST_QNAME,
      "some-ref");
  public static final QName MYIDENTITY_QNAME = QName.create(TEST_QNAME,
      "myidentity");
  public static final QName SWITCH_FEATURES_QNAME = QName.create(TEST_QNAME,
      "switch-features");

  public static final QName AUGMENTED_LIST_QNAME = QName.create(TEST_QNAME,
      "augmented-list");

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
  public static final YangInstanceIdentifier OUTER_LIST_PATH =
      YangInstanceIdentifier.builder(TEST_PATH).node(OUTER_LIST_QNAME).build();
  public static final QName TWO_QNAME = QName.create(TEST_QNAME, "two");
  public static final QName THREE_QNAME = QName.create(TEST_QNAME, "three");

  private static final Integer ONE_ID = 1;
  private static final Integer TWO_ID = 2;
  private static final String TWO_ONE_NAME = "one";
  private static final String TWO_TWO_NAME = "two";
  private static final String DESC = "Hello there";
  private static final Long LONG_ID = 1L;
  private static final Boolean ENABLED = false;
  private static final Short SHORT_ID = 1;
  private static final Byte BYTE_ID = 1;
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

  public static final YangInstanceIdentifier FAMILY_PATH =
      YangInstanceIdentifier.of(FAMILY_QNAME);
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


  // first child
  private static final YangInstanceIdentifier CHILDREN_1_PATH =
      YangInstanceIdentifier.builder(CHILDREN_PATH)
          .nodeWithKey(CHILDREN_QNAME, CHILD_NUMBER_QNAME, FIRST_CHILD_ID) //
          .build();
  private static final YangInstanceIdentifier CHILDREN_1_NAME_PATH =
      YangInstanceIdentifier.builder(CHILDREN_PATH)
          .nodeWithKey(CHILDREN_QNAME, CHILD_NAME_QNAME, FIRST_CHILD_NAME) //
          .build();

  private static final YangInstanceIdentifier CHILDREN_2_PATH =
      YangInstanceIdentifier.builder(CHILDREN_PATH)
          .nodeWithKey(CHILDREN_QNAME, CHILD_NUMBER_QNAME, SECOND_CHILD_ID) //
          .build();
  private static final YangInstanceIdentifier CHILDREN_2_NAME_PATH =
      YangInstanceIdentifier.builder(CHILDREN_PATH)
          .nodeWithKey(CHILDREN_QNAME, CHILD_NAME_QNAME, SECOND_CHILD_NAME) //
          .build();


  private static final YangInstanceIdentifier GRAND_CHILD_1_PATH =
      YangInstanceIdentifier
          .builder(CHILDREN_1_PATH)
          .node(GRAND_CHILDREN_QNAME)
          //
          .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME,
              FIRST_GRAND_CHILD_ID) //
          .build();

  private static final YangInstanceIdentifier GRAND_CHILD_1_NAME_PATH =
      YangInstanceIdentifier
          .builder(CHILDREN_1_PATH)
          .node(GRAND_CHILDREN_QNAME)
          //
          .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NAME_QNAME,
              FIRST_GRAND_CHILD_NAME) //
          .build();

  private static final YangInstanceIdentifier GRAND_CHILD_2_PATH =
      YangInstanceIdentifier
          .builder(CHILDREN_2_PATH)
          .node(GRAND_CHILDREN_QNAME)
          //
          .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME,
              SECOND_GRAND_CHILD_ID) //
          .build();

  private static final YangInstanceIdentifier GRAND_CHILD_2_NAME_PATH =
      YangInstanceIdentifier
          .builder(CHILDREN_2_PATH)
          .node(GRAND_CHILDREN_QNAME)
          //
          .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NAME_QNAME,
              SECOND_GRAND_CHILD_NAME) //
          .build();

  private static final YangInstanceIdentifier DESC_PATH_ID =
      YangInstanceIdentifier.builder(DESC_PATH).build();
  private static final YangInstanceIdentifier OUTER_LIST_1_PATH =
      YangInstanceIdentifier.builder(OUTER_LIST_PATH)
          .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, ONE_ID) //
          .build();

  private static final YangInstanceIdentifier OUTER_LIST_2_PATH =
      YangInstanceIdentifier.builder(OUTER_LIST_PATH)
          .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, TWO_ID) //
          .build();

  private static final YangInstanceIdentifier TWO_TWO_PATH =
      YangInstanceIdentifier.builder(OUTER_LIST_2_PATH).node(INNER_LIST_QNAME) //
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
   * <p/>
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

  public static DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> createBaseTestContainerBuilder() {
      // Create a list of shoes
      // This is to test leaf list entry
      final LeafSetEntryNode<Object> nike =
              ImmutableLeafSetEntryNodeBuilder
                      .create()
                      .withNodeIdentifier(
                              new YangInstanceIdentifier.NodeWithValue(QName.create(
                                      TEST_QNAME, "shoe"), "nike")).withValue("nike").build();

      final LeafSetEntryNode<Object> puma =
              ImmutableLeafSetEntryNodeBuilder
                      .create()
                      .withNodeIdentifier(
                              new YangInstanceIdentifier.NodeWithValue(QName.create(
                                      TEST_QNAME, "shoe"), "puma")).withValue("puma").build();

      final LeafSetNode<Object> shoes =
              ImmutableLeafSetNodeBuilder
                      .create()
                      .withNodeIdentifier(
                              new YangInstanceIdentifier.NodeIdentifier(QName.create(
                                      TEST_QNAME, "shoe"))).withChild(nike).withChild(puma)
                      .build();


      // Test a leaf-list where each entry contains an identity
      final LeafSetEntryNode<Object> cap1 =
              ImmutableLeafSetEntryNodeBuilder
                      .create()
                      .withNodeIdentifier(
                              new YangInstanceIdentifier.NodeWithValue(QName.create(
                                      TEST_QNAME, "capability"), DESC_QNAME))
                      .withValue(DESC_QNAME).build();

      final LeafSetNode<Object> capabilities =
              ImmutableLeafSetNodeBuilder
                      .create()
                      .withNodeIdentifier(
                              new YangInstanceIdentifier.NodeIdentifier(QName.create(
                                      TEST_QNAME, "capability"))).withChild(cap1).build();

      ContainerNode switchFeatures =
              ImmutableContainerNodeBuilder
                      .create()
                      .withNodeIdentifier(
                              new YangInstanceIdentifier.NodeIdentifier(SWITCH_FEATURES_QNAME))
                      .withChild(capabilities).build();

      // Create a leaf list with numbers
      final LeafSetEntryNode<Object> five =
              ImmutableLeafSetEntryNodeBuilder
                      .create()
                      .withNodeIdentifier(
                              (new YangInstanceIdentifier.NodeWithValue(QName.create(
                                      TEST_QNAME, "number"), 5))).withValue(5).build();
      final LeafSetEntryNode<Object> fifteen =
              ImmutableLeafSetEntryNodeBuilder
                      .create()
                      .withNodeIdentifier(
                              (new YangInstanceIdentifier.NodeWithValue(QName.create(
                                      TEST_QNAME, "number"), 15))).withValue(15).build();
      final LeafSetNode<Object> numbers =
              ImmutableLeafSetNodeBuilder
                      .create()
                      .withNodeIdentifier(
                              new YangInstanceIdentifier.NodeIdentifier(QName.create(
                                      TEST_QNAME, "number"))).withChild(five).withChild(fifteen)
                      .build();


      // Create augmentations
      MapEntryNode mapEntry = createAugmentedListEntry(1, "First Test");

      // Create a bits leaf
      NormalizedNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, Object, LeafNode<Object>>
              myBits = Builders.leafBuilder().withNodeIdentifier(
              new YangInstanceIdentifier.NodeIdentifier(
                      QName.create(TEST_QNAME, "my-bits"))).withValue(
              ImmutableSet.of("foo", "bar"));

      // Create unkey list entry
      UnkeyedListEntryNode binaryDataKey =
          Builders.unkeyedListEntryBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).
              withChild(ImmutableNodes.leafNode(SOME_BINARY_DATE_QNAME, DESC)).build();

      Map<QName, Object> keyValues = new HashMap<>();
      keyValues.put(CHILDREN_QNAME, FIRST_CHILD_NAME);


      // Create the document
      return ImmutableContainerNodeBuilder
              .create()
              .withNodeIdentifier(
                  new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME))
              .withChild(myBits.build())
              .withChild(ImmutableNodes.leafNode(DESC_QNAME, DESC))
              .withChild(ImmutableNodes.leafNode(POINTER_QNAME, ENABLED))
              .withChild(ImmutableNodes.leafNode(POINTER_QNAME, SHORT_ID))
              .withChild(ImmutableNodes.leafNode(POINTER_QNAME, BYTE_ID))
              .withChild(
                  ImmutableNodes.leafNode(SOME_REF_QNAME, GRAND_CHILD_1_PATH))
              .withChild(ImmutableNodes.leafNode(MYIDENTITY_QNAME, DESC_QNAME))
               .withChild(Builders.unkeyedListBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(OUTER_LIST_QNAME))
                   .withChild(binaryDataKey).build())
               .withChild(Builders.orderedMapBuilder()
                   .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME)).withChild(mapEntry).build())
               .withChild(Builders.choiceBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TEST_QNAME))
                   .withChild(ImmutableNodes.leafNode(DESC_QNAME, LONG_ID)).build())
                      // .withChild(augmentationNode)
              .withChild(shoes)
              .withChild(numbers)
              .withChild(switchFeatures)
              .withChild(
                  mapNodeBuilder(AUGMENTED_LIST_QNAME).withChild(mapEntry).build())
              .withChild(
                  mapNodeBuilder(OUTER_LIST_QNAME)
                      .withChild(mapEntry(OUTER_LIST_QNAME, ID_QNAME, ONE_ID))
                      .withChild(BAR_NODE).build()
              );
  }

  public static ContainerNode createTestContainer() {
      return createBaseTestContainerBuilder().build();
  }

  public static MapEntryNode createAugmentedListEntry(int id, String name) {

    Set<QName> childAugmentations = new HashSet<>();
    childAugmentations.add(AUG_CONT_QNAME);

    ContainerNode augCont =
        ImmutableContainerNodeBuilder
            .create()
            .withNodeIdentifier(
                new YangInstanceIdentifier.NodeIdentifier(AUG_CONT_QNAME))
            .withChild(ImmutableNodes.leafNode(AUG_NAME_QNAME, name)).build();


    final YangInstanceIdentifier.AugmentationIdentifier augmentationIdentifier =
        new YangInstanceIdentifier.AugmentationIdentifier(childAugmentations);

    final AugmentationNode augmentationNode =
        Builders.augmentationBuilder()
            .withNodeIdentifier(augmentationIdentifier).withChild(augCont)
            .build();

    return ImmutableMapEntryNodeBuilder
        .create()
        .withNodeIdentifier(
            new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                AUGMENTED_LIST_QNAME, ID_QNAME, id))
        .withChild(ImmutableNodes.leafNode(ID_QNAME, id))
        .withChild(augmentationNode).build();
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
