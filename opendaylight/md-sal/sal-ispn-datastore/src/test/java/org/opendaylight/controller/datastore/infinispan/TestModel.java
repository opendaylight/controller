package org.opendaylight.controller.datastore.infinispan;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetEntryNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableLeafSetNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntry;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapEntryBuilder;
import static org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes.mapNodeBuilder;

public class TestModel {

    public static final QName TEST_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", "2014-03-13",
            "test");
    public static final QName DESC_QNAME = QName.create(TEST_QNAME, "desc");
    public static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
    public static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
    public static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME, "outer-choice");
    public static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    public static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    public static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");
    private static final String DATASTORE_TEST_YANG = "/odl-datastore-test.yang";

    public static final InstanceIdentifier TEST_PATH = InstanceIdentifier.of(TEST_QNAME);
    public static final InstanceIdentifier DESC_PATH = InstanceIdentifier.builder(TEST_PATH).node(DESC_QNAME).build();
    public static final InstanceIdentifier OUTER_LIST_PATH = InstanceIdentifier.builder(TEST_PATH).node(OUTER_LIST_QNAME).build();
    public static final QName TWO_QNAME = QName.create(TEST_QNAME,"two");
    public static final QName THREE_QNAME = QName.create(TEST_QNAME,"three");

    private static final Integer ONE_ID = 1;
    private static final Integer TWO_ID = 2;
    private static final String TWO_ONE_NAME = "one";
    private static final String TWO_TWO_NAME = "two";
    private static final String DESC="Hello there";

    private static final InstanceIdentifier DESC_PATH_ID = InstanceIdentifier.builder(DESC_PATH).build();
    private static final InstanceIdentifier OUTER_LIST_1_PATH = InstanceIdentifier.builder(OUTER_LIST_PATH)
            .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, ONE_ID) //
            .build();

    private static final InstanceIdentifier OUTER_LIST_2_PATH = InstanceIdentifier.builder(OUTER_LIST_PATH)
            .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME, TWO_ID) //
            .build();

    private static final InstanceIdentifier TWO_TWO_PATH = InstanceIdentifier.builder(OUTER_LIST_2_PATH)
            .node(INNER_LIST_QNAME) //
            .nodeWithKey(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME) //
            .build();

    private static final InstanceIdentifier TWO_TWO_VALUE_PATH = InstanceIdentifier.builder(TWO_TWO_PATH)
            .node(VALUE_QNAME) //
            .build();

    private static final MapEntryNode BAR_NODE = mapEntryBuilder(OUTER_LIST_QNAME, ID_QNAME, TWO_ID) //
            .withChild(mapNodeBuilder(INNER_LIST_QNAME) //
                    .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_ONE_NAME)) //
                    .withChild(mapEntry(INNER_LIST_QNAME, NAME_QNAME, TWO_TWO_NAME)) //
                    .build()) //
            .build();

    public static final InputStream getDatastoreTestInputStream() {
        return getInputStream(DATASTORE_TEST_YANG);
    }

    private static InputStream getInputStream(final String resourceName) {
        return TestModel.class.getResourceAsStream(DATASTORE_TEST_YANG);
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
    public static NormalizedNode<?, ?> createDocumentOne(SchemaContext schemaContext) {
        return ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(schemaContext.getQName()))
                .withChild(createTestContainer()).build();

    }

    public static ContainerNode createTestContainer() {


        final LeafSetEntryNode<Object> nike = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(new InstanceIdentifier.NodeWithValue(QName.create(TEST_QNAME, "shoe"), "nike")).withValue("nike").build();
        final LeafSetEntryNode<Object> puma = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier(new InstanceIdentifier.NodeWithValue(QName.create(TEST_QNAME, "shoe"), "puma")).withValue("puma").build();
        final LeafSetNode<Object> shoes = ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(QName.create(TEST_QNAME, "shoe"))).withChild(nike).withChild(puma).build();


        final LeafSetEntryNode<Object> five = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier((new InstanceIdentifier.NodeWithValue(QName.create(TEST_QNAME, "number"), 5))).withValue(5).build();
        final LeafSetEntryNode<Object> fifteen = ImmutableLeafSetEntryNodeBuilder.create().withNodeIdentifier((new InstanceIdentifier.NodeWithValue(QName.create(TEST_QNAME, "number"), 15))).withValue(15).build();
        final LeafSetNode<Object> numbers = ImmutableLeafSetNodeBuilder.create().withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(QName.create(TEST_QNAME, "number"))).withChild(five).withChild(fifteen).build();

        return ImmutableContainerNodeBuilder
                .create()
                .withNodeIdentifier(new InstanceIdentifier.NodeIdentifier(TEST_QNAME))
                .withChild(ImmutableNodes.leafNode(DESC_QNAME, DESC))
                .withChild(shoes)
                .withChild(numbers)
                .withChild(
                        mapNodeBuilder(OUTER_LIST_QNAME)
                                .withChild(mapEntry(OUTER_LIST_QNAME, ID_QNAME, ONE_ID))
                                .withChild(BAR_NODE).build()).build();

    }

}