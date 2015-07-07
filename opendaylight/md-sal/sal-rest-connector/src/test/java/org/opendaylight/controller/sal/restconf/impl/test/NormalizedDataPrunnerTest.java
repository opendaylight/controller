package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import java.util.HashSet;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.NormalizedDataPrunner;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.AttributesContainer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedLeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

public class NormalizedDataPrunnerTest {

    private interface BadNodeTypeInterface<T> extends AttributesContainer, DataContainerChild<NodeIdentifier, T> {
    }

    private static class BadNodeType implements BadNodeTypeInterface<Object> {

        @Override
        public Map<QName, String> getAttributes() {
            return null;
        }

        @Override
        public Object getAttributeValue(final QName name) {
            return null;
        }

        @Override
        public QName getNodeType() {
            return null;
        }

        @Override
        public Object getValue() {
            return null;
        }

        @Override
        public NodeIdentifier getIdentifier() {
            return null;
        }

    }

    final NormalizedDataPrunner normalizedDataPrunner = new NormalizedDataPrunner();
    final LeafNode<Object> child = Builders.leafBuilder().withNodeIdentifier(toIdentifier("child")).withValue("child")
            .build();

    @Test
    public void nullDepthTest() {
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(null, null);
        assertEquals(null, node);
    }

    @Test(expected = IllegalStateException.class)
    public void badNodeType(){
        final BadNodeType badNode = new BadNodeType();
        normalizedDataPrunner.pruneDataAtDepth(badNode, 0);
    }

    @Test
    public void leafNodeTest() {
        final LeafNode<?> leafNode = Builders.leafBuilder().withNodeIdentifier(toIdentifier("leafNode"))
                .withValue("leafNode").build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(leafNode, 0);
        assertEquals(leafNode, node);
    }

    @Test
    public void leafSetNodeTest(){
        final LeafSetNode<Object> leafSetNode = Builders.leafSetBuilder()
                .withNodeIdentifier(toIdentifier("leafSetNode")).withChildValue("leafSetNode").build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(leafSetNode, 0);
        assertEquals(leafSetNode, node);
    }

    @Test
    public void anyXmlNodeTest() {
        final AnyXmlNode anyXmlNode = Builders.anyXmlBuilder().withNodeIdentifier(toIdentifier("anyXmlNode")).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(anyXmlNode, 0);
        assertEquals(anyXmlNode, node);
    }

    @Test
    public void orderedLeafSetNodeTest() {
        final OrderedLeafSetNode<Object> orderedLeafSetNode = (OrderedLeafSetNode<Object>) Builders
                .orderedLeafSetBuilder().withNodeIdentifier(toIdentifier("orderedLeafSetNode")).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(orderedLeafSetNode, 0);
        assertEquals(orderedLeafSetNode, node);
    }

    @Test
    public void mixinNodeAugmentationWithoutChildTest() {
        final AugmentationNode augmtation = Builders.augmentationBuilder()
                .withNodeIdentifier(toAugmIdentifier("augment")).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(augmtation, 0);
        assertEquals(augmtation, node);
    }

    @Test
    public void mixinNodeAugmentationWithChildTest() {
        final AugmentationNode augmtation = Builders.augmentationBuilder()
                .withNodeIdentifier(toAugmIdentifier("augment")).addChild(child).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(augmtation, 0);
        assertEquals(augmtation, node);
    }

    @Test
    public void mixinNodeChoiceWithoutChildTest() {
        final ChoiceNode choice = Builders.choiceBuilder().withNodeIdentifier(toIdentifier("choice"))
                .build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(choice, 0);
        assertEquals(choice, node);
    }

    @Test
    public void mixinNodeChoiceWithChildTest() {
        final ChoiceNode choice = Builders.choiceBuilder().withNodeIdentifier(toIdentifier("choice")).addChild(child)
                .build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(choice, 0);
        assertEquals(choice, node);
    }

    @Test
    public void mixinNodeEmptyOrderedMapNodeTest() {
        final OrderedMapNode orderedMapNode = Builders.orderedMapBuilder()
                .withNodeIdentifier(toIdentifier("orderedMapNode")).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(orderedMapNode, 0);
        assertEquals(orderedMapNode, node);
    }

    @Test
    public void mixinNodeFilledOrderedMapNodeTest() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> mapEntry = Builders
                .mapEntryBuilder().withNodeIdentifier(toNodeIdentifier("node"));
        final OrderedMapNode orderedMapNode = Builders.orderedMapBuilder()
                .withNodeIdentifier(toIdentifier("orderedMapNode")).withChild(mapEntry.build()).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(orderedMapNode, 2);
        assertEquals(orderedMapNode, node);
    }

    @Test
    public void mixinNodeEmptyMapNodeTest() {
        final MapNode mapNode = Builders.mapBuilder().withNodeIdentifier(toIdentifier("mapNode")).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(mapNode, 0);
        assertEquals(mapNode, node);
    }

    @Test
    public void mixinNodeFilledMapNodeTest() {
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> mapEntry = Builders
                .mapEntryBuilder().withNodeIdentifier(toNodeIdentifier("node"));
        final MapNode mapNode = Builders.mapBuilder().withNodeIdentifier(toIdentifier("mapNode"))
                .withChild(mapEntry.build()).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(mapNode, 2);
        assertEquals(mapNode, node);
    }

    @Test
    public void mixinNodeEmptyUnkeyedListTest() {
        final UnkeyedListNode unkeyedListNode = Builders.unkeyedListBuilder()
                .withNodeIdentifier(toIdentifier("unkeyedListNode")).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(unkeyedListNode, 0);
        assertEquals(unkeyedListNode.getIdentifier(), node.getIdentifier());
        assertEquals(unkeyedListNode.getNodeType(), node.getNodeType());
        assertEquals(unkeyedListNode.getValue(), node.getValue());
    }

    @Test
    public void mixinNodeFilledUnkeyedListTest() {
        final DataContainerNodeAttrBuilder<NodeIdentifier, UnkeyedListEntryNode> newUnkeyedListEntry = Builders
                .unkeyedListEntryBuilder().withNodeIdentifier(toIdentifier("value"));
        final UnkeyedListNode unkeyedListNode = Builders.unkeyedListBuilder()
                .withNodeIdentifier(toIdentifier("unkeyedListNode")).withChild(newUnkeyedListEntry.build()).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(unkeyedListNode, 2);
        assertEquals(unkeyedListNode.getIdentifier(), node.getIdentifier());
        assertEquals(unkeyedListNode.getNodeType(), node.getNodeType());
        assertEquals(unkeyedListNode.getValue(), node.getValue());
    }

    @Test
    public void emptyContainerTest() {
        final ContainerNode container = Builders.containerBuilder().withNodeIdentifier(toIdentifier("container"))
                .build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(container, 0);
        assertEquals(container, node);
    }

    @Test
    public void filledContainerTest() {
        final ContainerNode container = Builders.containerBuilder().withNodeIdentifier(toIdentifier("container"))
                .withChild(child).build();
        final DataContainerChild<?, ?> node = normalizedDataPrunner.pruneDataAtDepth(container, 2);
        assertEquals(container, node);
    }

    private NodeIdentifierWithPredicates toNodeIdentifier(final String string) {
        return new NodeIdentifierWithPredicates(QName.create("test", "2015-06-30", string), QName.create("test",
                "2015-06-30", "key"), string);
    }

    private AugmentationIdentifier toAugmIdentifier(final String string) {
        return new AugmentationIdentifier(new HashSet<QName>());
    }

    private NodeIdentifier toIdentifier(final String localName) {
        return new NodeIdentifier(QName.create("test", "2015-06-30", localName));
    }

}
