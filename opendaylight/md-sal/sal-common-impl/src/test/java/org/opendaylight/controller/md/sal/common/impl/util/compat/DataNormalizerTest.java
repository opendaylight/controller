package org.opendaylight.controller.md.sal.common.impl.util.compat;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class DataNormalizerTest {

    static final QName TEST_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:normalization:test",
                                                 "2014-03-13", "test");
    static final QName OUTER_LIST_QNAME = QName.create(TEST_QNAME, "outer-list");
    static final QName INNER_LIST_QNAME = QName.create(TEST_QNAME, "inner-list");
    static final QName OUTER_CHOICE_QNAME = QName.create(TEST_QNAME, "outer-choice");
    static final QName ID_QNAME = QName.create(TEST_QNAME, "id");
    static final QName NAME_QNAME = QName.create(TEST_QNAME, "name");
    static final QName VALUE_QNAME = QName.create(TEST_QNAME, "value");

    static final InstanceIdentifier TEST_PATH = InstanceIdentifier.of(TEST_QNAME);
    static final InstanceIdentifier OUTER_LIST_PATH = InstanceIdentifier.builder(TEST_PATH).node(OUTER_LIST_QNAME).build();
    static final QName ONE_QNAME = QName.create(TEST_QNAME,"one");
    static final QName TWO_QNAME = QName.create(TEST_QNAME,"two");
    static final QName THREE_QNAME = QName.create(TEST_QNAME,"three");

    static final QName ANY_XML_DATA_QNAME = QName.create(TEST_QNAME, "any-xml-data");
    static final QName OUTER_CONTAINER_QNAME = QName.create(TEST_QNAME, "outer-container");
    static final QName AUGMENTED_LEAF_QNAME = QName.create(TEST_QNAME, "augmented-leaf");
    static final QName UNKEYED_LIST_QNAME = QName.create(TEST_QNAME, "unkeyed-list");
    static final QName UNORDERED_LEAF_LIST_QNAME = QName.create(TEST_QNAME, "unordered-leaf-list");
    static final QName ORDERED_LEAF_LIST_QNAME = QName.create(TEST_QNAME, "ordered-leaf-list");

    static final Short OUTER_LIST_ID = (short)10;

    static final InstanceIdentifier OUTER_LIST_PATH_LEGACY = InstanceIdentifier.builder(TEST_QNAME)
            .nodeWithKey(OUTER_LIST_QNAME, ID_QNAME,OUTER_LIST_ID).build();

    static final InstanceIdentifier LEAF_TWO_PATH_LEGACY = InstanceIdentifier.builder(OUTER_LIST_PATH_LEGACY)
            .node(TWO_QNAME).build();

    static final QName ANY_XML_LEAF_QNAME = QName.create( TEST_QNAME, "leaf" );;
    static final QName ANY_XML_INNER_QNAME = QName.create( TEST_QNAME, "inner" );
    static final QName ANY_XML_INNER_LEAF_QNAME = QName.create( TEST_QNAME, "inner-leaf" );

    SchemaContext createTestContext() {
        YangParserImpl parser = new YangParserImpl();
        Set<Module> modules = parser.parseYangModelsFromStreams(Collections.singletonList(
                              DataNormalizerTest.class.getResourceAsStream( "/normalization-test.yang" )));
        return parser.resolveSchemaContext(modules);
    }

    @Test
    public void testToNormalizedInstanceIdentifier() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        InstanceIdentifier normalizedPath = normalizer.toNormalized(LEAF_TWO_PATH_LEGACY);

        verifyNormalizedInstanceIdentifier( normalizedPath, TEST_QNAME, OUTER_LIST_QNAME,
                new Object[]{OUTER_LIST_QNAME, ID_QNAME, OUTER_LIST_ID},
                OUTER_CHOICE_QNAME, TWO_QNAME);
    }

    private void verifyNormalizedInstanceIdentifier( InstanceIdentifier actual, Object... expPath  ) {

        assertNotNull( "Actual InstanceIdentifier is null", actual );
        assertEquals( "InstanceIdentifier path length", expPath.length, actual.getPath().size() );

        for( int i = 0; i < expPath.length; i++ ) {
            PathArgument actualArg = actual.getPath().get( i );
            if( expPath[i] instanceof Object[] ) {
                Object[] exp = (Object[])expPath[i];
                if( exp[0] instanceof Set ) { // AugmentationIdentifier
                    assertEquals( "Actual path arg " + (i + 1) + " class",
                                   AugmentationIdentifier.class,  actualArg.getClass() );
                    AugmentationIdentifier actualNode = (AugmentationIdentifier)actualArg;
                    assertEquals( "Actual path arg " + (i + 1) + " PossibleChildNames",
                                  exp[0], actualNode.getPossibleChildNames() );
                }
                else { // NodeIdentifierWithPredicates
                    assertEquals( "Actual path arg " + (i + 1) + " class",
                            NodeIdentifierWithPredicates.class,  actualArg.getClass() );
                    NodeIdentifierWithPredicates actualNode = (NodeIdentifierWithPredicates)actualArg;
                    assertEquals( "Actual path arg " + (i + 1) + " node type",
                            exp[0], actualNode.getNodeType() );
                    assertEquals( "Actual path arg " + (i + 1) + " key values map size",
                            1, actualNode.getKeyValues().size() );
                    Entry<QName, Object> keyValuesEntry = actualNode.getKeyValues().entrySet().iterator().next();
                    assertEquals( "Actual path arg " + (i + 1) + " key values map key",
                            exp[1], keyValuesEntry .getKey() );
                    assertEquals( "Actual path arg " + (i + 1) + " key values map value",
                            exp[2], keyValuesEntry .getValue() );
                }
            }
            else
            {
                assertEquals( "Actual path arg " + (i + 1) + " node type",
                              expPath[i], actualArg.getNodeType() );
            }
        }
    }

    @Test
    public void testToLegacyInstanceIdentifier() throws DataNormalizationException {

        DataNormalizer normalizer = new DataNormalizer( createTestContext() );

        InstanceIdentifier normalized = InstanceIdentifier.builder()
            .node( TEST_QNAME )
            .node( OUTER_LIST_QNAME )
            .nodeWithKey( OUTER_LIST_QNAME, ID_QNAME, OUTER_LIST_ID )
            .node( OUTER_CHOICE_QNAME ).node( TWO_QNAME ).build();

        InstanceIdentifier legacy = normalizer.toLegacy( normalized );

        assertEquals( "Legacy InstanceIdentifier", LEAF_TWO_PATH_LEGACY, legacy );
    }

    @Test
    public void testToLegacyNormalizedNode() {

        ChoiceNode choiceNode1 = Builders.choiceBuilder()
                .withNodeIdentifier( new NodeIdentifier( OUTER_CHOICE_QNAME ) )
                .withChild( ImmutableNodes.leafNode( TWO_QNAME, "two" ) )
                .withChild( ImmutableNodes.leafNode( THREE_QNAME, "three" ) ).build();

        MapEntryNode innerListEntryNode1 = Builders.mapEntryBuilder()
                .withNodeIdentifier( new NodeIdentifierWithPredicates(
                        INNER_LIST_QNAME, NAME_QNAME, "inner-name1" ) )
                .withChild( ImmutableNodes.leafNode( NAME_QNAME, "inner-name1" ) )
                .withChild( ImmutableNodes.leafNode( VALUE_QNAME, "inner-value1" ) ).build();

        MapEntryNode innerListEntryNode2 = Builders.mapEntryBuilder()
                .withNodeIdentifier( new NodeIdentifierWithPredicates(
                        INNER_LIST_QNAME, NAME_QNAME, "inner-name2" ) )
                .withChild( ImmutableNodes.leafNode( NAME_QNAME, "inner-name2" ) )
                .withChild( ImmutableNodes.leafNode( VALUE_QNAME, "inner-value2" ) ).build();

        OrderedMapNode innerListNode = Builders.orderedMapBuilder()
                .withNodeIdentifier( new NodeIdentifier( INNER_LIST_QNAME ) )
                .withChild( innerListEntryNode1 )
                .withChild( innerListEntryNode2 ).build();

        Short outerListID1 = Short.valueOf( (short)10 );
        MapEntryNode outerListEntryNode1 = Builders.mapEntryBuilder()
                .withNodeIdentifier( new NodeIdentifierWithPredicates(
                                         OUTER_LIST_QNAME, ID_QNAME, outerListID1 ) )
                .withChild( ImmutableNodes.leafNode( ID_QNAME, outerListID1 ) )
                .withChild( choiceNode1 )
                .withChild( innerListNode ).build();

        ChoiceNode choiceNode2 = Builders.choiceBuilder()
                .withNodeIdentifier( new NodeIdentifier( OUTER_CHOICE_QNAME ) )
                .withChild( ImmutableNodes.leafNode( ONE_QNAME, "one" ) ).build();

        Short outerListID2 = Short.valueOf( (short)20 );
        MapEntryNode outerListEntryNode2 = Builders.mapEntryBuilder()
                .withNodeIdentifier( new NodeIdentifierWithPredicates(
                                         OUTER_LIST_QNAME, ID_QNAME, outerListID2 ) )
                .withChild( ImmutableNodes.leafNode( ID_QNAME, outerListID2 ) )
                .withChild( choiceNode2 ).build();

        MapNode outerListNode = Builders.mapBuilder()
                .withNodeIdentifier( new NodeIdentifier( OUTER_LIST_QNAME ) )
                .withChild( outerListEntryNode1 )
                .withChild( outerListEntryNode2 ).build();

        UnkeyedListEntryNode unkeyedListEntryNode1 = Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier( new NodeIdentifier( UNKEYED_LIST_QNAME ) )
                .withChild( ImmutableNodes.leafNode( NAME_QNAME, "unkeyed1" ) ).build();

        UnkeyedListEntryNode unkeyedListEntryNode2 = Builders.unkeyedListEntryBuilder()
                .withNodeIdentifier( new NodeIdentifier( UNKEYED_LIST_QNAME ) )
                .withChild( ImmutableNodes.leafNode( NAME_QNAME, "unkeyed2" ) ).build();

        UnkeyedListNode unkeyedListNode = Builders.unkeyedListBuilder()
                .withNodeIdentifier( new NodeIdentifier( UNKEYED_LIST_QNAME ) )
                .withChild( unkeyedListEntryNode1 )
                .withChild( unkeyedListEntryNode2 ).build();

        ContainerNode testContainerNode = Builders.containerBuilder()
                .withNodeIdentifier( new NodeIdentifier( TEST_QNAME ) )
                .withChild( outerListNode )
                .withChild( unkeyedListNode ).build();

        Node<?> legacyNode = DataNormalizer.toLegacy( testContainerNode );

        verifyLegacyNode( legacyNode,
                TEST_QNAME, Lists.<Object[]>newArrayList(
                new Object[]{OUTER_LIST_QNAME, Lists.<Object[]>newArrayList(
                    new Object[]{ID_QNAME, outerListID1},
                    new Object[]{TWO_QNAME, "two"},
                    new Object[]{THREE_QNAME, "three"},

                    new Object[]{INNER_LIST_QNAME, Lists.<Object[]>newArrayList(
                        new Object[]{NAME_QNAME, "inner-name1"},
                        new Object[]{VALUE_QNAME, "inner-value1"}
                    ) },

                    new Object[]{INNER_LIST_QNAME, Lists.<Object[]>newArrayList(
                            new Object[]{NAME_QNAME, "inner-name2"},
                            new Object[]{VALUE_QNAME, "inner-value2"}
                    ) }
                ) },
                new Object[]{OUTER_LIST_QNAME, Lists.<Object[]>newArrayList(
                    new Object[]{ID_QNAME, outerListID2},
                    new Object[]{ONE_QNAME, "one"}
                ) },
                new Object[]{UNKEYED_LIST_QNAME, Lists.<Object[]>newArrayList(
                        new Object[]{NAME_QNAME, "unkeyed1"}
                ) },
                new Object[]{UNKEYED_LIST_QNAME, Lists.<Object[]>newArrayList(
                        new Object[]{NAME_QNAME, "unkeyed2"}
                ) }
            ) );

        // Conversion of Mixin type nodes is not supported.

        assertNull( "Expected null returned for Mixin type node",
                    DataNormalizer.toLegacy( outerListNode ) );
    }

    @Test
    public void testToLegacyNormalizedNodeWithAnyXml() {

        ContainerNode innerContainerNode = Builders.containerBuilder()
                .withNodeIdentifier( new NodeIdentifier( ANY_XML_INNER_QNAME ) )
                .withChild( ImmutableNodes.leafNode( ANY_XML_INNER_LEAF_QNAME, "inner-leaf-value" ) )
                .build();

        ContainerNode anyXmlContainerNode = Builders.containerBuilder()
                .withNodeIdentifier( new NodeIdentifier( ANY_XML_DATA_QNAME ) )
                .withChild( ImmutableNodes.leafNode( ANY_XML_LEAF_QNAME, "leaf-value" ) )
                .withChild( innerContainerNode ).build();

        ContainerNode testContainerNode = Builders.containerBuilder()
                .withNodeIdentifier( new NodeIdentifier( TEST_QNAME ) )
                .withChild( anyXmlContainerNode ).build();

        DataNormalizer normalizer = new DataNormalizer( createTestContext() );

        Node<?> legacyNode = normalizer.toLegacy(
                              InstanceIdentifier.builder( TEST_QNAME ).build(), testContainerNode );

        verifyLegacyNode( legacyNode,
            TEST_QNAME, Lists.<Object[]>newArrayList(
                new Object[]{ANY_XML_DATA_QNAME, Lists.<Object[]>newArrayList(
                    new Object[]{ANY_XML_LEAF_QNAME, "leaf-value"},

                    new Object[]{ANY_XML_INNER_QNAME, Lists.<Object[]>newArrayList(
                        new Object[]{ANY_XML_INNER_LEAF_QNAME, "inner-leaf-value"}
                    ) }
                ) }
            ) );
    }

    @Test
    public void testToLegacyNormalizedNodeWithLeafLists() {

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName( TEST_QNAME );

        ListNodeBuilder<Object, LeafSetEntryNode<Object>> leafSetBuilder = Builders.leafSetBuilder()
                .withNodeIdentifier( new NodeIdentifier( UNORDERED_LEAF_LIST_QNAME ) );
        for( int i = 1; i <= 3; i++ ) {
            leafSetBuilder.withChildValue( "unordered-value" + i );
        }

        ListNodeBuilder<Object, LeafSetEntryNode<Object>> orderedLeafSetBuilder = Builders.orderedLeafSetBuilder()
                 .withNodeIdentifier( new NodeIdentifier( ORDERED_LEAF_LIST_QNAME ) );
        for( int i = 3; i > 0; i-- ) {
            orderedLeafSetBuilder.withChildValue( "ordered-value" + i );
        }

        ContainerNode testContainerNode = Builders.containerBuilder()
                .withNodeIdentifier( new NodeIdentifier( TEST_QNAME ) )
                .withChild( leafSetBuilder.build() )
                .withChild( orderedLeafSetBuilder.build() ).build();

        DataNormalizer normalizer = new DataNormalizer( createTestContext() );

        Node<?> legacyNode = normalizer.toLegacy(
                              InstanceIdentifier.builder( TEST_QNAME ).build(), testContainerNode );

        verifyLegacyNode( legacyNode,
            TEST_QNAME, Lists.<Object[]>newArrayList(
                new Object[]{UNORDERED_LEAF_LIST_QNAME, "unordered-value1"},
                new Object[]{UNORDERED_LEAF_LIST_QNAME, "unordered-value2"},
                new Object[]{UNORDERED_LEAF_LIST_QNAME, "unordered-value3"},
                new Object[]{ORDERED_LEAF_LIST_QNAME, "ordered-value3"},
                new Object[]{ORDERED_LEAF_LIST_QNAME, "ordered-value2"},
                new Object[]{ORDERED_LEAF_LIST_QNAME, "ordered-value1"}
            ) );
    }

    @Test
    public void testToLegacyNormalizedNodeWithAugmentation() {

        AugmentationNode augmentationNode = Builders.augmentationBuilder()
                .withNodeIdentifier( new AugmentationIdentifier(
                                            Sets.newHashSet( AUGMENTED_LEAF_QNAME ) ) )
                .withChild( ImmutableNodes.leafNode( AUGMENTED_LEAF_QNAME, "augmented-value" ) )
                .build();

        ContainerNode outerContainerNode = Builders.containerBuilder()
                .withNodeIdentifier( new NodeIdentifier( OUTER_CONTAINER_QNAME ) )
                .withChild( augmentationNode ).build();

        ContainerNode testContainerNode = Builders.containerBuilder()
                .withNodeIdentifier( new NodeIdentifier( TEST_QNAME ) )
                .withChild( outerContainerNode ).build();

        DataNormalizer normalizer = new DataNormalizer( createTestContext() );

        Node<?> legacyNode = normalizer.toLegacy(
                              InstanceIdentifier.builder( TEST_QNAME ).build(), testContainerNode );

        verifyLegacyNode( legacyNode,
            TEST_QNAME, Lists.<Object[]>newArrayList(
                new Object[]{OUTER_CONTAINER_QNAME, Lists.<Object[]>newArrayList(
                    new Object[]{AUGMENTED_LEAF_QNAME, "augmented-value"}
                ) }
            ) );
    }

    private boolean isOrdered( QName nodeName ) {
        return ORDERED_LEAF_LIST_QNAME.equals( nodeName ) ||
               INNER_LIST_QNAME.equals( nodeName );
    }

    @SuppressWarnings("unchecked")
    private void verifyLegacyNode( Node<?> actual, QName key, List<Object[]> expNodeData ) {

        assertNotNull( "Actual Node is null", actual );
        assertTrue( "Expected CompositeNode instance", actual instanceof CompositeNode );
        CompositeNode actualCN = (CompositeNode)actual;
        assertEquals( "Node key", key, actualCN.getKey() );

        List<Object[]> expChildData = Lists.newArrayList();
        List<Object[]> unorderdChildData = Lists.newArrayList();
        for( Object[] data: expNodeData ) {
            if( isOrdered( (QName)data[0] ) ) {
                expChildData.add( data );
            }
            else {
                unorderdChildData.add( data );
            }
        }

        Collections.sort( unorderdChildData, new Comparator<Object[]>() {
            @Override
            public int compare( Object[] arg1, Object[] arg2 ) {
                String str1 = ((QName)arg1[0]).getLocalName();
                if( !(arg1[1] instanceof List) )
                    str1 += arg1[1]; // add simple node value

                String str2 = ((QName)arg2[0]).getLocalName();
                if( !(arg2[1] instanceof List) )
                    str2 += arg2[1]; // add simple node value

                return str1.compareTo( str2 );
            }
        } );

        expChildData.addAll( unorderdChildData );

        List<Node<?>> actualChildNodes = Lists.newArrayList();
        List<Node<?>> unorderedChildNodes = Lists.newArrayList();
        for( Node<?> node: actualCN.getValue() ) {
            if( isOrdered( node.getKey() ) ) {
                actualChildNodes.add( node );
            }
            else {
                unorderedChildNodes.add( node );
            }
        }

        Collections.sort( unorderedChildNodes, new Comparator<Node<?>>() {
            @Override
            public int compare( Node<?> n1, Node<?> n2 ) {
                String str1 = n1.getKey().getLocalName();
                if( n1 instanceof SimpleNode )
                    str1 += ((SimpleNode<?>)n1).getValue();

                String str2 = n2.getKey().getLocalName();
                if( n2 instanceof SimpleNode )
                    str2 += ((SimpleNode<?>)n2).getValue();

                return str1.compareTo( str2 );
            }
        } );

        actualChildNodes.addAll( unorderedChildNodes );

        for( Node<?> actualChild: actualChildNodes ) {
            Object[] expData = expChildData.isEmpty() ? null : expChildData.remove( 0 );
            assertNotNull( "Unexpected child node with key " + actualChild.getKey(), expData );
            assertEquals( "Child node QName", expData[0], actualChild.getKey() );

            if( expData[1] instanceof List ) { // List represents a composite node
                verifyLegacyNode( actualChild, actualChild.getKey(), (List<Object[]>)expData[1] );
            }
            else { //  else a simple node
                assertTrue( "Expected SimpleNode instance", actualChild instanceof SimpleNode );
                assertEquals( "Child node value with key " + actualChild.getKey(), expData[1],
                              ((SimpleNode<?>)actualChild).getValue() );
            }
        }

        if( !expChildData.isEmpty() ) {
            fail( "Missing child nodes: " +
                  FluentIterable.from( expChildData ).transform( new Function<Object[],String>() {
                      @Override
                      public String apply( Object[] input ) {
                          return input[0].toString();
                      }
                  } ) );
        }
    }

    @Test
    public void testToNormalizedCompositeNode() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName( TEST_QNAME );

        CompositeNodeBuilder<ImmutableCompositeNode> outerListBuilder = ImmutableCompositeNode.builder();
        outerListBuilder.setQName( OUTER_LIST_QNAME );
        outerListBuilder.addLeaf( ID_QNAME, 10 );
        outerListBuilder.addLeaf( ONE_QNAME, "one" );

        for( int i = 3; i > 0; i-- ) {
            CompositeNodeBuilder<ImmutableCompositeNode> innerListBuilder = ImmutableCompositeNode.builder();
            innerListBuilder.setQName( INNER_LIST_QNAME );
            innerListBuilder.addLeaf( NAME_QNAME, "inner-name" + i );
            innerListBuilder.addLeaf( VALUE_QNAME, "inner-value" + i );
            outerListBuilder.add( innerListBuilder.toInstance() );
        }

        testBuilder.add( outerListBuilder.toInstance() );

        outerListBuilder = ImmutableCompositeNode.builder();
        outerListBuilder.setQName( OUTER_LIST_QNAME );
        outerListBuilder.addLeaf( ID_QNAME, 20 );
        outerListBuilder.addLeaf( TWO_QNAME, "two" );
        outerListBuilder.addLeaf( THREE_QNAME, "three" );
        testBuilder.add( outerListBuilder.toInstance() );

        for( int i = 1; i <= 2; i++ ) {
            CompositeNodeBuilder<ImmutableCompositeNode> unkeyedListBuilder = ImmutableCompositeNode.builder();
            unkeyedListBuilder.setQName( UNKEYED_LIST_QNAME );
            unkeyedListBuilder.addLeaf( NAME_QNAME, "unkeyed-name" + i );
            testBuilder.add( unkeyedListBuilder.toInstance() );
        }

        Entry<InstanceIdentifier, NormalizedNode<?, ?>> normalizedNodeEntry = normalizer.toNormalized(
                            new AbstractMap.SimpleEntry<InstanceIdentifier, CompositeNode>(
                                     new InstanceIdentifier( ImmutableList.<PathArgument>of(
                                                 new NodeIdentifier( TEST_QNAME ) ) ),
                                      testBuilder.toInstance() ) );

        verifyNormalizedInstanceIdentifier( normalizedNodeEntry.getKey(), TEST_QNAME );

        verifyNormalizedNode( normalizedNodeEntry.getValue(),
            new NodeIdentifier( TEST_QNAME ), ContainerNode.class, expectChildNodes(
                expectMapNode( OUTER_LIST_QNAME,
                    expectMapEntryNode( OUTER_LIST_QNAME, ID_QNAME, 10,
                        expectLeafNode( ID_QNAME, 10 ),
                        expectChoiceNode( OUTER_CHOICE_QNAME,
                            expectLeafNode( ONE_QNAME, "one" )
                        ),
                        expectOrderedMapNode( INNER_LIST_QNAME,
                            expectMapEntryNode( INNER_LIST_QNAME, NAME_QNAME, "inner-name3",
                                expectLeafNode( NAME_QNAME, "inner-name3" ),
                                expectLeafNode( VALUE_QNAME, "inner-value3" )
                            ),
                            expectMapEntryNode( INNER_LIST_QNAME, NAME_QNAME, "inner-name2",
                                expectLeafNode( NAME_QNAME, "inner-name2" ),
                                expectLeafNode( VALUE_QNAME, "inner-value2" )
                            ),
                            expectMapEntryNode( INNER_LIST_QNAME, NAME_QNAME, "inner-name1",
                                expectLeafNode( NAME_QNAME, "inner-name1" ),
                                expectLeafNode( VALUE_QNAME, "inner-value1" )
                            )
                        )
                    ),
                    expectMapEntryNode( OUTER_LIST_QNAME, ID_QNAME, 20,
                        expectLeafNode( ID_QNAME, 20 ),
                        expectChoiceNode( OUTER_CHOICE_QNAME,
                            expectLeafNode( TWO_QNAME, "two" ),
                            expectLeafNode( THREE_QNAME, "three" )
                        )
                    )
                ),
                expectUnkeyedListNode( UNKEYED_LIST_QNAME,
                    expectUnkeyedListEntryNode( UNKEYED_LIST_QNAME,
                        expectLeafNode( NAME_QNAME, "unkeyed-name1" )
                    ),
                    expectUnkeyedListEntryNode( UNKEYED_LIST_QNAME,
                        expectLeafNode( NAME_QNAME, "unkeyed-name2" )
                    )
                )
            ) );
    }

    @Test
    public void testToNormalizedCompositeNodeWithAnyXml() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName( TEST_QNAME );

        CompositeNodeBuilder<ImmutableCompositeNode> anyXmlBuilder = ImmutableCompositeNode.builder();
        anyXmlBuilder.setQName( ANY_XML_DATA_QNAME );
        anyXmlBuilder.addLeaf( ANY_XML_LEAF_QNAME, "leaf-value" );

        CompositeNodeBuilder<ImmutableCompositeNode> innerBuilder = ImmutableCompositeNode.builder();
        innerBuilder.setQName( ANY_XML_INNER_QNAME );
        innerBuilder.addLeaf( ANY_XML_INNER_LEAF_QNAME, "inner-leaf-value" );

        anyXmlBuilder.add( innerBuilder.toInstance() );
        testBuilder.add( anyXmlBuilder.toInstance() );

        Entry<InstanceIdentifier, NormalizedNode<?, ?>> normalizedNodeEntry = normalizer.toNormalized(
                            new AbstractMap.SimpleEntry<InstanceIdentifier, CompositeNode>(
                                     new InstanceIdentifier( ImmutableList.<PathArgument>of(
                                                 new NodeIdentifier( TEST_QNAME ) ) ),
                                      testBuilder.toInstance() ) );

        verifyNormalizedInstanceIdentifier( normalizedNodeEntry.getKey(), TEST_QNAME );

        verifyNormalizedNode( normalizedNodeEntry.getValue(),
            new NodeIdentifier( TEST_QNAME ), ContainerNode.class, expectChildNodes(
                expectContainerNode( ANY_XML_DATA_QNAME,
                    expectLeafNode( ANY_XML_LEAF_QNAME, "leaf-value" ),
                    expectContainerNode( ANY_XML_INNER_QNAME,
                         expectLeafNode( ANY_XML_INNER_LEAF_QNAME, "inner-leaf-value" )
                    )
                )
            ) );
    }

    @Test
    public void testToNormalizedCompositeNodeWithAugmentation() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName( TEST_QNAME );

        CompositeNodeBuilder<ImmutableCompositeNode> outerContBuilder = ImmutableCompositeNode.builder();
        outerContBuilder.setQName( OUTER_CONTAINER_QNAME );
        outerContBuilder.addLeaf( AUGMENTED_LEAF_QNAME, "augmented-value" );

        testBuilder.add( outerContBuilder.toInstance() );

        Entry<InstanceIdentifier, NormalizedNode<?, ?>> normalizedNodeEntry = normalizer.toNormalized(
                            new AbstractMap.SimpleEntry<InstanceIdentifier, CompositeNode>(
                                     new InstanceIdentifier( ImmutableList.<PathArgument>of(
                                                 new NodeIdentifier( TEST_QNAME ) ) ),
                                     testBuilder.toInstance() ) );

        verifyNormalizedInstanceIdentifier( normalizedNodeEntry.getKey(), TEST_QNAME );

        Object[] expAugmentation = expectAugmentation( AUGMENTED_LEAF_QNAME,
            expectLeafNode( AUGMENTED_LEAF_QNAME, "augmented-value" ) );

        verifyNormalizedNode( normalizedNodeEntry.getValue(),
            new NodeIdentifier( TEST_QNAME ), ContainerNode.class, expectChildNodes(
                expectContainerNode( OUTER_CONTAINER_QNAME,
                    expAugmentation
                )
            ) );

        normalizedNodeEntry = normalizer.toNormalized(
                new AbstractMap.SimpleEntry<InstanceIdentifier, CompositeNode>(
                         new InstanceIdentifier( Lists.newArrayList(
                                 new NodeIdentifier( TEST_QNAME ),
                                 new NodeIdentifier( OUTER_CONTAINER_QNAME ) ) ),
                         outerContBuilder.toInstance() ) );

        verifyNormalizedInstanceIdentifier( normalizedNodeEntry.getKey(),
                TEST_QNAME, OUTER_CONTAINER_QNAME,
                new Object[]{Sets.newHashSet( AUGMENTED_LEAF_QNAME )});

        verifyNormalizedNode( normalizedNodeEntry.getValue(),
                (PathArgument)expAugmentation[0], (Class<?>) expAugmentation[1], expAugmentation[2] );
    }

    @Test
    public void testToNormalizedCompositeNodeWithLeafLists() {
        SchemaContext testCtx = createTestContext();
        DataNormalizer normalizer = new DataNormalizer(testCtx);

        CompositeNodeBuilder<ImmutableCompositeNode> testBuilder = ImmutableCompositeNode.builder();
        testBuilder.setQName( TEST_QNAME );

        for( int i = 1; i <= 3; i++ ) {
            testBuilder.addLeaf( UNORDERED_LEAF_LIST_QNAME, "unordered-value" + i );
        }

        for( int i = 3; i > 0; i-- ) {
            testBuilder.addLeaf( ORDERED_LEAF_LIST_QNAME, "ordered-value" + i );
        }

        Entry<InstanceIdentifier, NormalizedNode<?, ?>> normalizedNodeEntry = normalizer.toNormalized(
                            new AbstractMap.SimpleEntry<InstanceIdentifier, CompositeNode>(
                                     new InstanceIdentifier( ImmutableList.<PathArgument>of(
                                                 new NodeIdentifier( TEST_QNAME ) ) ),
                                     testBuilder.toInstance() ) );

        verifyNormalizedInstanceIdentifier( normalizedNodeEntry.getKey(), TEST_QNAME );

        verifyNormalizedNode( normalizedNodeEntry.getValue(),
            new NodeIdentifier( TEST_QNAME ), ContainerNode.class, expectChildNodes(
                expectLeafSetNode( UNORDERED_LEAF_LIST_QNAME,
                    expectLeafSetEntryNode( UNORDERED_LEAF_LIST_QNAME, "unordered-value1" ),
                    expectLeafSetEntryNode( UNORDERED_LEAF_LIST_QNAME, "unordered-value2" ),
                    expectLeafSetEntryNode( UNORDERED_LEAF_LIST_QNAME, "unordered-value3" )
                ),
                expectOrderedLeafSetNode( ORDERED_LEAF_LIST_QNAME,
                    expectLeafSetEntryNode( ORDERED_LEAF_LIST_QNAME, "ordered-value3" ),
                    expectLeafSetEntryNode( ORDERED_LEAF_LIST_QNAME, "ordered-value2" ),
                    expectLeafSetEntryNode( ORDERED_LEAF_LIST_QNAME, "ordered-value1" )
                )
            ) );
    }

    @SuppressWarnings("unchecked")
    private void verifyNormalizedNode( NormalizedNode<?, ?> actual, PathArgument expNodeID,
                                       Class<?> expNodeClass, Object expValueData ) {

        assertNotNull( "Actual NormalizedNode is null", actual );
        assertTrue( "NormalizedNode instance " + actual.getClass() + " is not derived from " + expNodeClass,
                    expNodeClass.isAssignableFrom( actual.getClass() ) );
        assertEquals( "NormalizedNode identifier", expNodeID, actual.getIdentifier() );

        if( expValueData instanceof List ) {
            Map<PathArgument,Integer> orderingMap = null;
            if( expNodeClass.equals( OrderedMapNode.class ) ||
                expNodeClass.equals( OrderedLeafSetNode.class ) ) {
                orderingMap = Maps.newHashMap();
            }

            int i = 1;
            Map<PathArgument,Object[]> expChildMap = Maps.newHashMap();
            List<Object[]> expValueList = (List<Object[]>)expValueData;
            for( Object[] data: expValueList ) {
                expChildMap.put( (PathArgument) data[0], data );

                if( orderingMap != null ) {
                    orderingMap.put( (PathArgument) data[0], i++ );
                }
            }

            assertNotNull( "Actual value is null for node " + actual.getIdentifier(), actual.getValue() );
            assertTrue( "Expected value instance Iterable for node " + actual.getIdentifier(),
                        Iterable.class.isAssignableFrom( actual.getValue().getClass() ) );

            i = 1;
            for( NormalizedNode<?,?> actualChild: (Iterable<NormalizedNode<?,?>>)actual.getValue() ) {
                Object[] expChildData = expNodeClass.equals( UnkeyedListNode.class ) ?
                                               expValueList.remove( 0 ) :
                                               expChildMap.remove( actualChild.getIdentifier() );

                assertNotNull( "Unexpected child node " + actualChild.getClass() +
                               " with identifier " + actualChild.getIdentifier() +
                               " for parent node " + actual.getClass() +
                               " with identifier " + actual.getIdentifier(), expChildData );

                if( orderingMap != null ) {
                    assertEquals( "Order index for child node " + actualChild.getIdentifier(),
                                  orderingMap.get( actualChild.getIdentifier() ), Integer.valueOf( i ) );
                }

                verifyNormalizedNode( actualChild, (PathArgument)expChildData[0],
                                      (Class<?>)expChildData[1], expChildData[2] );

                i++;
            }

            if( expNodeClass.equals( UnkeyedListNode.class ) ) {
                if( expValueList.size() > 0 ) {
                    fail( "Missing " + expValueList.size() + " child nodes for parent " +
                          actual.getIdentifier() );
                }
            }
            else {
                if( !expChildMap.isEmpty() ) {
                    fail( "Missing child nodes for parent " + actual.getIdentifier() +
                            ": " + expChildMap.keySet() );
                }
            }
        }
        else {
            assertEquals( "Leaf value for node " + actual.getIdentifier(),
                          expValueData, actual.getValue() );
        }
    }

    private Object[] expectOrderedLeafSetNode( QName nodeName, Object[]... childData ) {
        return new Object[]{new NodeIdentifier( nodeName ),
                            OrderedLeafSetNode.class, Lists.newArrayList( childData )};
    }

    private Object[] expectLeafSetNode( QName nodeName, Object[]... childData ) {
        return new Object[]{new NodeIdentifier( nodeName ),
                            LeafSetNode.class, Lists.newArrayList( childData )};
    }

    private Object[] expectLeafSetEntryNode( QName nodeName, Object value ) {
        return new Object[]{new NodeWithValue( nodeName, value ), LeafSetEntryNode.class, value};
    }

    private Object[] expectUnkeyedListNode( QName nodeName, Object[]... childData ) {
        return new Object[]{new NodeIdentifier( nodeName ),
                            UnkeyedListNode.class, Lists.newArrayList( childData )};
    }

    private Object[] expectUnkeyedListEntryNode( QName nodeName, Object[]... childData ) {
        return new Object[]{new NodeIdentifier( nodeName ),
                            UnkeyedListEntryNode.class, Lists.newArrayList( childData )};
    }

    private Object[] expectAugmentation( QName augmentedNodeName, Object[]... childData ) {
        return new Object[]{new AugmentationIdentifier( Sets.newHashSet( augmentedNodeName ) ),
                            AugmentationNode.class, Lists.newArrayList( childData )};
    }

    private Object[] expectContainerNode( QName nodeName, Object[]... childData ) {
        return new Object[]{new NodeIdentifier( nodeName ),
                            ContainerNode.class, Lists.newArrayList( childData )};
    }

    private Object[] expectChoiceNode( QName nodeName, Object[]... childData ) {
        return new Object[]{new NodeIdentifier( nodeName ),
                            ChoiceNode.class, Lists.newArrayList( childData )};
    }

    private Object[] expectLeafNode( QName nodeName, Object value ) {
        return new Object[]{new NodeIdentifier( nodeName ), LeafNode.class, value};

    }

    private Collection<Object[]> expectChildNodes( Object[]... childData ) {
        return Lists.newArrayList( childData );
    }

    private Object[] expectMapEntryNode( QName nodeName, QName key, Object value, Object[]... childData ) {
        return new Object[]{new NodeIdentifierWithPredicates( nodeName, key, value ),
                            MapEntryNode.class, Lists.newArrayList( childData )};
    }

    private Object[] expectMapNode( QName key, Object[]... childData ) {
        return new Object[]{new NodeIdentifier( key ), MapNode.class, Lists.newArrayList( childData )};
    }

    private Object[] expectOrderedMapNode( QName key, Object[]... childData ) {
        return new Object[]{new NodeIdentifier( key ), OrderedMapNode.class,
                            Lists.newArrayList( childData )};
    }
}
