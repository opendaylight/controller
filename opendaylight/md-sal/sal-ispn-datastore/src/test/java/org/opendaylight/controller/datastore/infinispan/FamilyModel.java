package org.opendaylight.controller.datastore.infinispan;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.*;


public class FamilyModel {

    public static final QName TEST_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:notification-test", "2014-04-15",
            "family");
    public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
    public static final QName CHILDREN_QNAME = QName.create(TEST_QNAME, "children");
    public static final QName GRAND_CHILDREN_QNAME = QName.create(TEST_QNAME, "grand-children");
    public static final QName CHILD_NUMBER_QNAME = QName.create(TEST_QNAME, "child-number");
    public static final QName CHILD_NAME_QNAME = QName.create(TEST_QNAME, "child-name");
    public static final QName GRAND_CHILD_NUMBER_QNAME = QName.create(TEST_QNAME, "grand-child-number");
    public static final QName GRAND_CHILD_NAME_QNAME  = QName.create(TEST_QNAME,"grand-child-name");
    private static final String DATASTORE_TEST_YANG = "/odl-datastore-test-notification.yang";

    public static final InstanceIdentifier TEST_PATH = InstanceIdentifier.of(TEST_QNAME);
    public static final InstanceIdentifier DESC_PATH = InstanceIdentifier.builder(TEST_PATH).node(DESC_QNAME).build();
    public static final InstanceIdentifier CHILDREN_PATH = InstanceIdentifier.builder(TEST_PATH).node(CHILDREN_QNAME).build();
    public static final QName TWO_QNAME = QName.create(TEST_QNAME,"two");
    public static final QName THREE_QNAME = QName.create(TEST_QNAME,"three");

    private static final Integer FIRST_CHILD_ID = 1;
    private static final Integer SECOND_CHILD_ID = 2;

    private static final String FIRST_CHILD_NAME = "first child";
    private static final String SECOND_CHILD_NAME = "second child";

  private static final Integer FIRST_GRAND_CHILD_ID = 1;
  private static final Integer SECOND_GRAND_CHILD_ID = 2;

  private static final String FIRST_GRAND_CHILD_NAME = "first grand child";
  private static final String SECOND_GRAND_CHILD_NAME = "second grand child";

    //first child
    private static final InstanceIdentifier CHILDREN_1_PATH = InstanceIdentifier.builder(CHILDREN_PATH)
            .nodeWithKey(CHILDREN_QNAME, CHILD_NUMBER_QNAME, FIRST_CHILD_ID) //
            .build();
    private static final InstanceIdentifier CHILDREN_1_NAME_PATH = InstanceIdentifier.builder(CHILDREN_PATH)
      .nodeWithKey(CHILDREN_QNAME, CHILD_NAME_QNAME, FIRST_CHILD_NAME) //
      .build();

    private static final InstanceIdentifier CHILDREN_2_PATH = InstanceIdentifier.builder(CHILDREN_PATH)
      .nodeWithKey(CHILDREN_QNAME, CHILD_NUMBER_QNAME, SECOND_CHILD_ID) //
      .build();
    private static final InstanceIdentifier CHILDREN_2_NAME_PATH = InstanceIdentifier.builder(CHILDREN_PATH)
      .nodeWithKey(CHILDREN_QNAME, CHILD_NAME_QNAME, SECOND_CHILD_NAME) //
      .build();


    private static final InstanceIdentifier GRAND_CHILD_1_PATH = InstanceIdentifier.builder(CHILDREN_1_PATH)
            .node(GRAND_CHILDREN_QNAME) //
            .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID) //
            .build();

  private static final InstanceIdentifier GRAND_CHILD_1_NAME_PATH = InstanceIdentifier.builder(CHILDREN_1_PATH)
      .node(GRAND_CHILDREN_QNAME) //
      .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NAME_QNAME, FIRST_GRAND_CHILD_NAME) //
      .build();

  private static final InstanceIdentifier GRAND_CHILD_2_PATH = InstanceIdentifier.builder(CHILDREN_2_PATH)
      .node(GRAND_CHILDREN_QNAME) //
      .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME, SECOND_GRAND_CHILD_ID) //
      .build();

  private static final InstanceIdentifier GRAND_CHILD_2_NAME_PATH = InstanceIdentifier.builder(CHILDREN_2_PATH)
      .node(GRAND_CHILDREN_QNAME) //
      .nodeWithKey(GRAND_CHILDREN_QNAME, GRAND_CHILD_NAME_QNAME, SECOND_GRAND_CHILD_NAME) //
      .build();

   static final MapEntryNode GRAND_CHILDREN_1 = mapEntryBuilder(CHILDREN_QNAME,GRAND_CHILDREN_QNAME,FIRST_GRAND_CHILD_ID)
                                            .withChild(mapNodeBuilder(GRAND_CHILDREN_QNAME)
                                             .withChild(mapEntry(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME, FIRST_GRAND_CHILD_ID))
        .withChild(mapEntry(GRAND_CHILDREN_QNAME, GRAND_CHILD_NAME_QNAME, FIRST_GRAND_CHILD_NAME))
        .build()).build();


  static final MapEntryNode GRAND_CHILDREN_2 = mapEntryBuilder(CHILDREN_QNAME,GRAND_CHILDREN_QNAME,SECOND_GRAND_CHILD_ID)
      .withChild(mapNodeBuilder(GRAND_CHILDREN_QNAME)
          .withChild(mapEntry(GRAND_CHILDREN_QNAME, GRAND_CHILD_NUMBER_QNAME, SECOND_GRAND_CHILD_ID))
          .withChild(mapEntry(GRAND_CHILDREN_QNAME, GRAND_CHILD_NAME_QNAME,SECOND_GRAND_CHILD_NAME))
          .build()).build();




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
     *
     * <pre>
     * family
     *     children
     *         child-number 1
     *         child-name "first child"
     *         grand-children
     *            grand-child-number 1
     *            grand child-name "first grand child"
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


    public static ContainerNode createTestContainer() {

        return ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(TEST_QNAME))
                .withChild(ImmutableNodes.leafNode(DESC_QNAME, "family tree"))
                .withChild(
                    mapNodeBuilder(CHILDREN_QNAME)
                        .withChild(mapEntry(CHILDREN_QNAME, CHILD_NUMBER_QNAME, FIRST_CHILD_ID))
                        .withChild(mapEntry(CHILDREN_QNAME, CHILD_NAME_QNAME, FIRST_CHILD_NAME))
                        .withChild(GRAND_CHILDREN_1)
                        .withChild(mapEntry(CHILDREN_QNAME, CHILD_NUMBER_QNAME, SECOND_CHILD_ID))
                        .withChild(mapEntry(CHILDREN_QNAME, CHILD_NAME_QNAME, SECOND_CHILD_NAME))
                        .withChild(GRAND_CHILDREN_2).build())
                        .build();



    }

}