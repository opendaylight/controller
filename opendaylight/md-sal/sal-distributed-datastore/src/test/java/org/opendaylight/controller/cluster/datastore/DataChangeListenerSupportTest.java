/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.INNER_LIST_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.OUTER_CONTAINER_PATH;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.OUTER_CONTAINER_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.OUTER_LIST_PATH;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.OUTER_LIST_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.TEST_PATH;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.TEST_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerEntryPath;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerEntryKey;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerEntryPath;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerNodeEntry;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.testNodeWithOuter;
import akka.actor.ActorRef;
import akka.testkit.TestActorRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.utils.MockDataChangeListener;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Unit tests for DataChangeListenerSupport.
 *
 * @author Thomas Pantelis
 */
public class DataChangeListenerSupportTest extends AbstractShardTest {
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private Shard shard;
    private DataChangeListenerSupport support;

    @Before
    public void setup() throws InterruptedException {
        shard = createShard();
        support = new DataChangeListenerSupport(shard);
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        actorFactory.close();
    }

    @Test
    public void testChangeListenerWithNoInitialData() throws Exception {
        MockDataChangeListener listener = registerChangeListener(TEST_PATH, DataChangeScope.ONE, 0, true);

        listener.expectNoMoreChanges("Unexpected initial change event");
    }

    @Test
    public void testInitialChangeListenerEventWithContainerPath() throws Exception {
        writeToStore(shard.getDataStore(), TEST_PATH, ImmutableNodes.containerNode(TEST_QNAME));

        MockDataChangeListener listener = registerChangeListener(TEST_PATH, DataChangeScope.ONE, 1, true);

        listener.waitForChangeEvents(TEST_PATH);
    }

    @Test
    public void testInitialChangeListenerEventWithListPath() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));

        MockDataChangeListener listener = registerChangeListener(OUTER_LIST_PATH, DataChangeScope.ONE, 1, true);

        listener.waitForChangeEvents();
        assertEquals("Outer entry 1 present", true, NormalizedNodes.findNode(
                listener.getCreatedData(0, OUTER_LIST_PATH), outerEntryKey(1)).isPresent());
        assertEquals("Outer entry 2 present", true, NormalizedNodes.findNode(
                listener.getCreatedData(0, OUTER_LIST_PATH), outerEntryKey(2)).isPresent());
    }

    @Test
    public void testInitialChangeListenerEventWithWildcardedListPath() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));
        writeToStore(shard.getDataStore(), OUTER_CONTAINER_PATH, ImmutableNodes.containerNode(OUTER_CONTAINER_QNAME));

        MockDataChangeListener listener = registerChangeListener(OUTER_LIST_PATH.node(OUTER_LIST_QNAME),
                DataChangeScope.ONE, 1, true);

        listener.waitForChangeEvents();
        listener.verifyCreatedData(0, outerEntryPath(1));
        listener.verifyCreatedData(0, outerEntryPath(2));
        listener.verifyNoCreatedData(0, OUTER_CONTAINER_PATH);
    }

    @Test
    public void testInitialChangeListenerEventWithNestedWildcardedListsPath() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(outerNode(
                outerNodeEntry(1, innerNode("one", "two")), outerNodeEntry(2, innerNode("three", "four")))));

        MockDataChangeListener listener = registerChangeListener(
                OUTER_LIST_PATH.node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME),
                DataChangeScope.ONE, 1, true);

        listener.waitForChangeEvents();
        listener.verifyCreatedData(0, innerEntryPath(1, "one"));
        listener.verifyCreatedData(0, innerEntryPath(1, "two"));
        listener.verifyCreatedData(0, innerEntryPath(2, "three"));
        listener.verifyCreatedData(0, innerEntryPath(2, "four"));

        // Register for a specific outer list entry

        MockDataChangeListener listener2 = registerChangeListener(
                OUTER_LIST_PATH.node(outerEntryKey(1)).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME),
                DataChangeScope.ONE, 1, true);

        listener2.waitForChangeEvents();
        listener2.verifyCreatedData(0, innerEntryPath(1, "one"));
        listener2.verifyCreatedData(0, innerEntryPath(1, "two"));
        listener2.verifyNoCreatedData(0, innerEntryPath(2, "three"));
        listener2.verifyNoCreatedData(0, innerEntryPath(2, "four"));
    }

    @Test
    public void testInitialChangeListenerEventWhenNotInitiallyLeader() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(outerNode(
                outerNodeEntry(1, innerNode("one", "two")), outerNodeEntry(2, innerNode("three", "four")))));

        MockDataChangeListener listener = registerChangeListener(
                OUTER_LIST_PATH.node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME),
                DataChangeScope.ONE, 0, false);

        listener.expectNoMoreChanges("Unexpected initial change event");
        listener.reset(1);

        support.onLeadershipChange(true, true);

        listener.waitForChangeEvents();
        listener.verifyCreatedData(0, innerEntryPath(1, "one"));
        listener.verifyCreatedData(0, innerEntryPath(1, "two"));
        listener.verifyCreatedData(0, innerEntryPath(2, "three"));
        listener.verifyCreatedData(0, innerEntryPath(2, "four"));
    }

    private MockDataChangeListener registerChangeListener(final YangInstanceIdentifier path, final DataChangeScope scope,
            final int expectedEvents, final boolean isLeader) {
        MockDataChangeListener listener = new MockDataChangeListener(expectedEvents);
        ActorRef dclActor = actorFactory.createActor(DataChangeListener.props(listener));

        support.onMessage(new RegisterChangeListener(path, dclActor, scope, false), isLeader, true);
        return listener;
    }

    private Shard createShard() {
        TestActorRef<Shard> actor = actorFactory.createTestActor(newShardProps());
        return actor.underlyingActor();
    }
}
