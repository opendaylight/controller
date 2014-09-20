/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.Collection;
import java.util.Map;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.opendaylight.controller.md.sal.dom.store.impl.DatastoreTestTask.WriteTransactionCustomizer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.top.level.list.NestedList;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class AbstractDataChangeListenerTest {

    protected static final YangInstanceIdentifier TOP_LEVEL = YangInstanceIdentifier
            .of(Top.QNAME);
    private static final QName NAME_QNAME = QName.create(Top.QNAME, "name");
    protected static final String FOO = "foo";
    protected static final String BAR = "bar";
    protected static final String BAZ = "baz";

    private InMemoryDOMDataStore datastore;
    private SchemaContext schemaContext;
    private TestDCLExecutorService dclExecutorService;

    @Before
    public final void setup() throws Exception {
        YangModuleInfo moduleInfo = BindingReflections
                .getModuleInfo(TwoLevelList.class);
        ModuleInfoBackedContext context = ModuleInfoBackedContext.create();
        context.registerModuleInfo(moduleInfo);
        schemaContext = context.tryToCreateSchemaContext().get();

        dclExecutorService = new TestDCLExecutorService(
                SpecialExecutors.newBlockingBoundedFastThreadPool(1, 10, "DCL" ));

        datastore = new InMemoryDOMDataStore("TEST", dclExecutorService);
        datastore.onGlobalContextUpdated(schemaContext);
    }

    @After
    public void tearDown() {
        if( dclExecutorService != null ) {
            dclExecutorService.shutdownNow();
        }
    }

    /**
     * Create a new test task. The task will operate on the backed database,
     * and will use the proper background executor service.
     *
     * @return Test task initialized to clean up {@value #TOP_LEVEL} and its
     *         children.
     */
    public final DatastoreTestTask newTestTask() {
        return new DatastoreTestTask(datastore, dclExecutorService).cleanup(DatastoreTestTask
                .simpleDelete(TOP_LEVEL));
    }


    public static final YangInstanceIdentifier path(final String topName,
            final String nestedName) {
        return path(topName).node(NestedList.QNAME).node(
                new NodeIdentifierWithPredicates(NestedList.QNAME, NAME_QNAME,
                        nestedName));
    }

    public static final YangInstanceIdentifier path(final String topName) {
        return TOP_LEVEL.node(TopLevelList.QNAME).node(
                new NodeIdentifierWithPredicates(TopLevelList.QNAME,
                        NAME_QNAME, topName));
    }

    protected static DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> top() {
        return Builders.containerBuilder().withNodeIdentifier(
                new NodeIdentifier(Top.QNAME));
    }



    protected static void assertEmpty(final Collection<?> set) {
        Assert.assertTrue(set.isEmpty());
    }

    protected static void assertEmpty(final Map<?,?> set) {
        Assert.assertTrue(set.isEmpty());
    }

    protected static <K> void assertContains(final Collection<K> set, final K... values) {
        for (K key : values) {
            Assert.assertTrue(set.contains(key));
        }

    }

    protected static <K> void assertNotContains(final Collection<K> set, final K... values) {
        for (K key : values) {
            Assert.assertFalse(set.contains(key));
        }
    }

    protected static <K> void assertContains(final Map<K,?> map, final K... values) {
        for (K key : values) {
            Assert.assertTrue(map.containsKey(key));
        }
    }

    protected static <K> void assertNotContains(final Map<K,?> map, final K... values) {
        for (K key : values) {
            Assert.assertFalse(map.containsKey(key));
        }
    }

    protected static CollectionNodeBuilder<MapEntryNode, MapNode> topLevelMap() {
        return ImmutableNodes.mapNodeBuilder(TopLevelList.QNAME);
    }

    protected static CollectionNodeBuilder<MapEntryNode, OrderedMapNode> nestedMap() {
        return Builders.orderedMapBuilder().withNodeIdentifier(new NodeIdentifier(NestedList.QNAME));
    }

    public static DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> topLevelList(
            final String key) {
        return ImmutableNodes.mapEntryBuilder(TopLevelList.QNAME, NAME_QNAME,
                key);
    }

    public static DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> nestedList(
            final String key) {
        return ImmutableNodes
                .mapEntryBuilder(NestedList.QNAME, NAME_QNAME, key);
    }

    public static final WriteTransactionCustomizer writeOneTopMultipleNested(
            final String topName, final String... nestedName) {
        CollectionNodeBuilder<MapEntryNode, OrderedMapNode> nestedMapBuilder = nestedMap();
        for (String nestedItem : nestedName) {
            nestedMapBuilder.addChild(nestedList(nestedItem).build());
        }

        final ContainerNode data = top().addChild(
                topLevelMap().addChild(
                        topLevelList(topName)
                                .addChild(nestedMapBuilder.build()).build())
                        .build()).build();

        return DatastoreTestTask.simpleWrite(TOP_LEVEL, data);
    }

    public static final  WriteTransactionCustomizer deleteNested(final String topName,
            final String nestedName) {
        return DatastoreTestTask.simpleDelete(path(topName, nestedName));
    }
}
