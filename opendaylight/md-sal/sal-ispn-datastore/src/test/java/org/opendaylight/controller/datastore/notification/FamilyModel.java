package org.opendaylight.controller.datastore.notification;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntryBuilder;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapNodeBuilder;


public class FamilyModel {

  public static final QName TEST_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test", "2014-04-17",
      "family");
  public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
  public static final QName CHILDREN_QNAME = QName.create(TEST_QNAME, "children");
  public static final QName GRAND_CHILDREN_QNAME = QName.create(TEST_QNAME, "grand-children");
  public static final QName CHILD_NUMBER_QNAME = QName.create(TEST_QNAME, "child-number");
  public static final QName CHILD_NAME_QNAME = QName.create(TEST_QNAME, "child-name");
  public static final QName GRAND_CHILD_NUMBER_QNAME = QName.create(TEST_QNAME, "grand-child-number");
  public static final QName GRAND_CHILD_NAME_QNAME = QName.create(TEST_QNAME, "grand-child-name");
  private static final String DATASTORE_TEST_YANG = "/odl-datastore-test-notification.yang";

  public static final InstanceIdentifier TEST_PATH = InstanceIdentifier.of(TEST_QNAME);
  public static final InstanceIdentifier DESC_PATH = InstanceIdentifier.builder(TEST_PATH).node(DESC_QNAME).build();
  public static final InstanceIdentifier CHILDREN_PATH = InstanceIdentifier.builder(TEST_PATH).node(CHILDREN_QNAME).build();
  public static final QName TWO_QNAME = QName.create(TEST_QNAME, "two");
  public static final QName THREE_QNAME = QName.create(TEST_QNAME, "three");

  private static final Integer FIRST_CHILD_ID = 1;
  private static final Integer SECOND_CHILD_ID = 2;

  public static final String FIRST_CHILD_NAME = "first child";
  private static final String SECOND_CHILD_NAME = "second child";

  private static final Integer FIRST_GRAND_CHILD_ID = 1;
  private static final Integer SECOND_GRAND_CHILD_ID = 2;

  private static final String FIRST_GRAND_CHILD_NAME = "first grand child";
  private static final String SECOND_GRAND_CHILD_NAME = "second grand child";

  //first child
  private static final InstanceIdentifier CHILDREN_1_PATH = InstanceIdentifier.builder(CHILDREN_PATH)
      .nodeWithKey(CHILDREN_QNAME, CHILD_NUMBER_QNAME, FIRST_CHILD_ID) //
      .build();


  public static final InputStream getDatastoreTestInputStream() {
    return getInputStream(DATASTORE_TEST_YANG);
  }

  private static InputStream getInputStream(final String resourceName) {
    return FamilyModel.class.getResourceAsStream(DATASTORE_TEST_YANG);
  }

  public static SchemaContext createTestContext() {
    YangParserImpl parser = new YangParserImpl();
    Set<Module> modules = parser.parseYangModelsFromStreams(Collections.singletonList(getDatastoreTestInputStream()));
    return parser.resolveSchemaContext(modules);
  }

  /**
   * Returns a test document
   * <p/>
   * <pre>
   * family
   *     children
   *         child-number 1
   *         child-name "first child"
   *         grand-children
   *            grand-child-number 1
   *            grand child-name "first grand child"
   *     children
   *         child-number 2
   *         child-name "second child"
   *         grand-children
   *            grand-child-number 2
   *            grand child-name "second grand child"
   *
   * </pre>
   *
   * @return
   */
  public static NormalizedNode<?, ?> createDocumentOne(SchemaContext schemaContext) {
    return ImmutableContainerNodeBuilder
        .create()
        .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(schemaContext.getQName()))
        .withChild(createTestContainer()).build();

  }

  public static ContainerNode createTestContainer() {
    final DataContainerNodeAttrBuilder<InstanceIdentifier.NodeIdentifier, ContainerNode> familyContainerBuilder = ImmutableContainerNodeBuilder
        .create()
        .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(TEST_QNAME));

    final CollectionNodeBuilder<MapEntryNode, MapNode> childrenBuilder = mapNodeBuilder(CHILDREN_QNAME);

    final DataContainerNodeBuilder<InstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> firstChildBuilder = mapEntryBuilder(CHILDREN_QNAME, CHILD_NUMBER_QNAME, FIRST_CHILD_ID);
    final DataContainerNodeBuilder<InstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> secondChildBuilder = mapEntryBuilder(CHILDREN_QNAME, CHILD_NUMBER_QNAME, SECOND_CHILD_ID);

    final DataContainerNodeBuilder<InstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> firstGrandChildBuilder = mapEntryBuilder(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID);
    final DataContainerNodeBuilder<InstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> secondGrandChildBuilder = mapEntryBuilder(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME, SECOND_GRAND_CHILD_ID);

    firstGrandChildBuilder.withChild(ImmutableNodes.leafNode(GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(GRAND_CHILD_NAME_QNAME, FIRST_GRAND_CHILD_NAME));

    secondGrandChildBuilder.withChild(ImmutableNodes.leafNode(GRAND_CHILD_NUMBER_QNAME, SECOND_GRAND_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(GRAND_CHILD_NAME_QNAME, SECOND_GRAND_CHILD_NAME));

    firstChildBuilder.withChild(ImmutableNodes.leafNode(CHILD_NUMBER_QNAME, FIRST_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(CHILD_NAME_QNAME, FIRST_CHILD_NAME))
        .withChild(mapNodeBuilder(GRAND_CHILDREN_QNAME).withChild(firstGrandChildBuilder.build()).build());


    secondChildBuilder.withChild(ImmutableNodes.leafNode(CHILD_NUMBER_QNAME, SECOND_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(CHILD_NAME_QNAME, SECOND_CHILD_NAME))
        .withChild(mapNodeBuilder(GRAND_CHILDREN_QNAME).withChild(firstGrandChildBuilder.build()).build());

    childrenBuilder.withChild(firstChildBuilder.build());
    childrenBuilder.withChild(secondChildBuilder.build());

    return familyContainerBuilder.withChild(childrenBuilder.build()).build();
  }


  public static ContainerNode createTestContainerWithFirstChildNameChanged(String firstChildName) {
    final DataContainerNodeAttrBuilder<InstanceIdentifier.NodeIdentifier, ContainerNode> familyContainerBuilder = ImmutableContainerNodeBuilder
        .create()
        .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(TEST_QNAME));

    final CollectionNodeBuilder<MapEntryNode, MapNode> childrenBuilder = mapNodeBuilder(CHILDREN_QNAME);

    final DataContainerNodeBuilder<InstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> firstChildBuilder = mapEntryBuilder(CHILDREN_QNAME, CHILD_NUMBER_QNAME, FIRST_CHILD_ID);
    final DataContainerNodeBuilder<InstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> secondChildBuilder = mapEntryBuilder(CHILDREN_QNAME, CHILD_NUMBER_QNAME, SECOND_CHILD_ID);

    final DataContainerNodeBuilder<InstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> firstGrandChildBuilder = mapEntryBuilder(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID);
    final DataContainerNodeBuilder<InstanceIdentifier.NodeIdentifierWithPredicates, MapEntryNode> secondGrandChildBuilder = mapEntryBuilder(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME, SECOND_GRAND_CHILD_ID);

    firstGrandChildBuilder.withChild(ImmutableNodes.leafNode(GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(GRAND_CHILD_NAME_QNAME, FIRST_GRAND_CHILD_NAME));

    secondGrandChildBuilder.withChild(ImmutableNodes.leafNode(GRAND_CHILD_NUMBER_QNAME, SECOND_GRAND_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(GRAND_CHILD_NAME_QNAME, SECOND_GRAND_CHILD_NAME));

    firstChildBuilder.withChild(ImmutableNodes.leafNode(CHILD_NUMBER_QNAME, FIRST_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(CHILD_NAME_QNAME, firstChildName))
        .withChild(mapNodeBuilder(GRAND_CHILDREN_QNAME).withChild(firstGrandChildBuilder.build()).build());


    secondChildBuilder.withChild(ImmutableNodes.leafNode(CHILD_NUMBER_QNAME, SECOND_CHILD_ID))
        .withChild(ImmutableNodes.leafNode(CHILD_NAME_QNAME, SECOND_CHILD_NAME))
        .withChild(mapNodeBuilder(GRAND_CHILDREN_QNAME).withChild(firstGrandChildBuilder.build()).build());

    childrenBuilder.withChild(firstChildBuilder.build());
    childrenBuilder.withChild(secondChildBuilder.build());

    return familyContainerBuilder.withChild(childrenBuilder.build()).build();
  }

}