/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.Lists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.OrderedContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.UnkeyedContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.UnkeyedContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.UnorderedContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.ordered.container.OrderedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.ordered.container.OrderedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.ordered.container.OrderedListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unkeyed.container.UnkeyedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unkeyed.container.UnkeyedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.store.rev140422.lists.unordered.container.UnorderedListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.OrderedMapNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;

/*
 * FIXME: THis test should be moved to sal-binding-broker and rewriten
 * to use new DataBroker API
 */
public class ListProcessingAndOrderingTest extends AbstractDataServiceTest {

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier DOM_UNORDERED_LIST_PATH = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
            .builder().node(Lists.QNAME).node(UnorderedContainer.QNAME).node(UnorderedList.QNAME).build();

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier DOM_ORDERED_LIST_PATH = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
            .builder().node(Lists.QNAME).node(OrderedContainer.QNAME).node(OrderedList.QNAME).build();


    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier DOM_UNKEYED_LIST_PATH = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier
            .builder().node(Lists.QNAME).node(UnkeyedContainer.QNAME).node(UnkeyedList.QNAME).build();

    private static final InstanceIdentifier<UnorderedContainer> UNORDERED_CONTAINER_PATH = InstanceIdentifier.builder(Lists.class).child(UnorderedContainer.class).build();
    private static final InstanceIdentifier<OrderedContainer> ORDERED_CONTAINER_PATH = InstanceIdentifier.builder(Lists.class).child(OrderedContainer.class).build();
    private static final InstanceIdentifier<UnkeyedContainer> UNKEYED_CONTAINER_PATH = InstanceIdentifier.builder(Lists.class).child(UnkeyedContainer.class).build();

    private static final UnorderedListKey UNORDERED_FOO_KEY = new UnorderedListKey("foo");
    private static final UnorderedListKey UNORDERED_BAR_KEY = new UnorderedListKey("bar");

    private static final InstanceIdentifier<UnorderedList> UNORDERED_FOO_PATH = UNORDERED_CONTAINER_PATH.child(UnorderedList.class,UNORDERED_FOO_KEY);
    private static final InstanceIdentifier<UnorderedList> UNORDERED_BAR_PATH = UNORDERED_CONTAINER_PATH.child(UnorderedList.class,UNORDERED_BAR_KEY);

    private static final OrderedListKey ORDERED_FOO_KEY = new OrderedListKey("foo");
    private static final OrderedListKey ORDERED_BAR_KEY = new OrderedListKey("bar");
    private static final InstanceIdentifier<OrderedList> ORDERED_FOO_PATH = ORDERED_CONTAINER_PATH.child(OrderedList.class,ORDERED_FOO_KEY);
    private static final InstanceIdentifier<OrderedList> ORDERED_BAR_PATH = ORDERED_CONTAINER_PATH.child(OrderedList.class,ORDERED_BAR_KEY);


    @Test
    public void unorderedListReadWriteTest() throws InterruptedException, ExecutionException {
        DataModificationTransaction tx = baDataService.beginTransaction();

        tx.putOperationalData(UNORDERED_FOO_PATH, createUnordered("foo"));
        tx.putOperationalData(UNORDERED_BAR_PATH, createUnordered("bar"));
        assertedCommit(tx);

        NormalizedNode<?, ?> data = resolveDataAsserted(DOM_UNORDERED_LIST_PATH);
        assertTrue(data instanceof MapNode);
        assertFalse(data instanceof OrderedMapNode);

        assertXmlRepresentation(UNORDERED_CONTAINER_PATH, "foo","bar");
    }



    @Test
    public void orderedListReadWriteTest() throws InterruptedException, ExecutionException {
        DataModificationTransaction tx = baDataService.beginTransaction();

        tx.putOperationalData(ORDERED_FOO_PATH, createOrdered("foo"));
        tx.putOperationalData(ORDERED_BAR_PATH, createOrdered("bar"));
        assertedCommit(tx);
        NormalizedNode<?, ?> data = resolveDataAsserted(DOM_ORDERED_LIST_PATH);
        assertTrue(data instanceof MapNode);
        assertTrue(data instanceof OrderedMapNode);

        assertXmlRepresentation(ORDERED_CONTAINER_PATH, "foo","bar");

    }



    @Test
    public void unkeyedListReadWriteTest() throws InterruptedException, ExecutionException {
        DataModificationTransaction tx = baDataService.beginTransaction();

        ImmutableList<UnkeyedList> unkeyedItems= ImmutableList.<UnkeyedList>builder()
                .add(createUnkeyed("foo"))
                .add(createUnkeyed("bar"))
                .build();

        tx.putOperationalData(UNKEYED_CONTAINER_PATH, new UnkeyedContainerBuilder().setUnkeyedList(unkeyedItems).build());
        assertedCommit(tx);
        NormalizedNode<?, ?> data = resolveDataAsserted(DOM_UNKEYED_LIST_PATH);
        assertFalse(data instanceof MapNode);
        assertTrue(data instanceof UnkeyedListNode);

        assertXmlRepresentation(UNKEYED_CONTAINER_PATH, "foo","bar");
    }

    private NormalizedNode<?, ?> resolveDataAsserted(
            final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier domPath) {

        try (DOMDataReadOnlyTransaction readTx = testContext.getDomAsyncDataBroker().newReadOnlyTransaction()){
            ListenableFuture<Optional<NormalizedNode<?, ?>>> data = readTx.read(LogicalDatastoreType.OPERATIONAL, domPath);
            Optional<NormalizedNode<?, ?>> potential = data.get();
            assertTrue(potential.isPresent());
            return potential.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static UnorderedList createUnordered(final String name) {
        return new UnorderedListBuilder().setName(name).setValue(createValue(name)).build();
    }

    private static OrderedList createOrdered(final String name) {
        return new OrderedListBuilder().setName(name).setValue(createValue(name)).build();
    }

    private static UnkeyedList createUnkeyed(final String name) {
        return new UnkeyedListBuilder().setName(name).setValue(createValue(name)).build();
    }

    private static String createValue(final String name) {
        return name + "-" + name.hashCode();
    }

    private static void assertedCommit(final DataModificationTransaction tx) throws InterruptedException, ExecutionException {
        RpcResult<TransactionStatus> result = tx.commit().get();
        assertTrue(result.isSuccessful());
        assertEquals(TransactionStatus.COMMITED,result.getResult());
    }

    private void assertXmlRepresentation(final InstanceIdentifier<?> containerPath, final String... childNameValues) {

        org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier domPath = testContext.getBindingToDomMappingService().toDataDom(containerPath);
        CompositeNode compositeNode = testContext.getDomDataBroker().readOperationalData(domPath);
        assertNotNull(compositeNode);

        Set<String> childValues = new HashSet<>();
        Collections.addAll(childValues, childNameValues);

        for(Node<?> child : compositeNode.getChildren()) {
            assertTrue(child instanceof CompositeNode);
            CompositeNode compChild = (CompositeNode) child;
            String nameLeafValue = (String) compChild.getSimpleNodesByName("name").get(0).getValue();
            String valueLeafValue = (String) compChild.getSimpleNodesByName("value").get(0).getValue();

            assertEquals(createValue(nameLeafValue), valueLeafValue);
            childValues.remove(nameLeafValue);
        }
        assertTrue(childValues.isEmpty());
    }

}
