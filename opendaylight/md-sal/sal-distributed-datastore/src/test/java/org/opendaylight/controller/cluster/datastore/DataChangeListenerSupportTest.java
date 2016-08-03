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
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerEntryPath;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerEntryKey;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerEntryPath;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerNodeEntry;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.testNodeWithOuter;
import akka.actor.ActorRef;
import akka.dispatch.Dispatchers;
import akka.testkit.TestActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.utils.MockDataChangeListener;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Unit tests for DataChangeListenerSupport.
 *
 * @author Thomas Pantelis
 */
public class DataChangeListenerSupportTest extends AbstractShardTest {

    @Test
    public void testChangeListenerWithNoInitialData() throws Exception {

        new ShardTestKit(getSystem()) {
            {
                final TestActorRef<Shard> actor = actorFactory.createTestActor(
                        newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        "testChangeListenerWithNoInitialData");

                waitUntilLeader(actor);

                final Shard shard = actor.underlyingActor();
                final MockDataChangeListener listener = new MockDataChangeListener(0);
                final ActorRef dclActor = actorFactory.createActor(DataChangeListener.props(listener),
                        "testChangeListenerWithNoInitialData-DataChangeListener");
                final DataChangeListenerSupport support = new DataChangeListenerSupport(shard);
                support.onMessage(new RegisterChangeListener(TEST_PATH, dclActor, DataChangeScope.ONE, false),true,true);

                listener.expectNoMoreChanges("Unexpected initial change event");
            }
        };
    }

    @Test
    public void testInitialChangeListenerEventWithContainerPath() throws Exception {

        new ShardTestKit(getSystem()) {
            {
                final TestActorRef<Shard> actor = actorFactory.createTestActor(
                        newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        "testInitialChangeListenerEventWithContainerPath");

                waitUntilLeader(actor);

                final Shard shard = actor.underlyingActor();
                writeToStore(shard.getDataStore(), TestModel.TEST_PATH,
                        ImmutableNodes.containerNode(TestModel.TEST_QNAME));
                final MockDataChangeListener listener = new MockDataChangeListener(1);
                final ActorRef dclActor = actorFactory.createActor(DataChangeListener.props(listener),
                        "testInitialChangeListenerEventWithContainerPath-DataChangeListener");
                final DataChangeListenerSupport support = new DataChangeListenerSupport(shard);
                support.onMessage(new RegisterChangeListener(TEST_PATH, dclActor, DataChangeScope.ONE, false),true,true);

                listener.waitForChangeEvents(TEST_PATH);
            }
        };
    }

    @Test
    public void testInitialChangeListenerEventWithListPath() throws Exception {
        new ShardTestKit(getSystem()) {
            {
                final TestActorRef<Shard> actor = actorFactory.createTestActor(
                        newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        "testInitialChangeListenerEventWithListPath");

                waitUntilLeader(actor);

                final Shard shard = actor.underlyingActor();
                mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));

                final MockDataChangeListener listener = new MockDataChangeListener(1);
                final ActorRef dclActor = actorFactory.createActor(DataChangeListener.props(listener),
                        "testInitialChangeListenerEventWithListPath-DataChangeListener");
                final DataChangeListenerSupport support = new DataChangeListenerSupport(shard);
                support.onMessage(new RegisterChangeListener(OUTER_LIST_PATH, dclActor, DataChangeScope.ONE, false),
                        true, true);

                listener.waitForChangeEvents();
                assertEquals("Outer entry 1 present", true, NormalizedNodes
                        .findNode(listener.getCreatedData(0, OUTER_LIST_PATH), outerEntryKey(1)).isPresent());
                assertEquals("Outer entry 2 present", true, NormalizedNodes
                        .findNode(listener.getCreatedData(0, OUTER_LIST_PATH), outerEntryKey(2)).isPresent());
            }
        };
    }

    @Test
    public void testInitialChangeListenerEventWithWildcardedListPath() throws Exception {

        new ShardTestKit(getSystem()) {
            {
                final TestActorRef<Shard> actor = actorFactory.createTestActor(
                        newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        "testInitialChangeListenerEventWithWildcardedListPath");

                waitUntilLeader(actor);

                final Shard shard = actor.underlyingActor();

                mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));
                writeToStore(shard.getDataStore(), OUTER_CONTAINER_PATH,
                        ImmutableNodes.containerNode(OUTER_CONTAINER_QNAME));

                final MockDataChangeListener listener = new MockDataChangeListener(1);
                final ActorRef dclActor = actorFactory.createActor(DataChangeListener.props(listener),
                        "testInitialChangeListenerEventWithWildcardedListPath-DataChangeListener");
                final DataChangeListenerSupport support = new DataChangeListenerSupport(shard);
                support.onMessage(new RegisterChangeListener(OUTER_LIST_PATH.node(OUTER_LIST_QNAME), dclActor,
                        DataChangeScope.ONE, false), true, true);

                listener.waitForChangeEvents();
                listener.verifyCreatedData(0, outerEntryPath(1));
                listener.verifyCreatedData(0, outerEntryPath(2));
                listener.verifyNoCreatedData(0, OUTER_CONTAINER_PATH);
            }
        };
    }

    @Test
    public void testInitialChangeListenerEventWithNestedWildcardedListsPath() throws Exception {

        new ShardTestKit(getSystem()) {
            {
                final TestActorRef<Shard> actor = actorFactory.createTestActor(
                        newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        "testInitialChangeListenerEventWithNestedWildcardedListsPath");

                waitUntilLeader(actor);

                final Shard shard = actor.underlyingActor();

                mergeToStore(shard.getDataStore(), TEST_PATH,
                        testNodeWithOuter(outerNode(outerNodeEntry(1, innerNode("one", "two")),
                                outerNodeEntry(2, innerNode("three", "four")))));

                final MockDataChangeListener listener = new MockDataChangeListener(1);
                final ActorRef dclActor = actorFactory.createActor(DataChangeListener.props(listener),
                        "testInitialChangeListenerEventWithNestedWildcardedListsPath-DataChangeListener");
                final DataChangeListenerSupport support = new DataChangeListenerSupport(shard);
                support.onMessage(new RegisterChangeListener(OUTER_LIST_PATH.node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME), dclActor,
                        DataChangeScope.ONE, false), true, true);


                listener.waitForChangeEvents();
                listener.verifyCreatedData(0, innerEntryPath(1, "one"));
                listener.verifyCreatedData(0, innerEntryPath(1, "two"));
                listener.verifyCreatedData(0, innerEntryPath(2, "three"));
                listener.verifyCreatedData(0, innerEntryPath(2, "four"));

                // Register for a specific outer list entry
                final MockDataChangeListener listener2 = new MockDataChangeListener(1);
                final ActorRef dclActor2 = actorFactory.createActor(DataChangeListener.props(listener2),
                        "testInitialChangeListenerEventWithNestedWildcardedListsPath-DataChangeListener2");
                final DataChangeListenerSupport support2 = new DataChangeListenerSupport(shard);
                support2.onMessage(new RegisterChangeListener(
                        OUTER_LIST_PATH.node(outerEntryKey(1)).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME), dclActor2,
                        DataChangeScope.ONE, false), true, true);

                listener2.waitForChangeEvents();
                listener2.verifyCreatedData(0, innerEntryPath(1, "one"));
                listener2.verifyCreatedData(0, innerEntryPath(1, "two"));
                listener2.verifyNoCreatedData(0, innerEntryPath(2, "three"));
                listener2.verifyNoCreatedData(0, innerEntryPath(2, "four"));
            }
        };
    }

    @Test
    public void testInitialChangeListenerEventWhenNotInitiallyLeader() throws Exception {

        new ShardTestKit(getSystem()) {
            {
                final TestActorRef<Shard> actor = actorFactory.createTestActor(
                        newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                        "testInitialChangeListenerEventWhenNotInitiallyLeader");

                waitUntilLeader(actor);

                final Shard shard = actor.underlyingActor();

                mergeToStore(shard.getDataStore(), TEST_PATH,
                        testNodeWithOuter(outerNode(outerNodeEntry(1, innerNode("one", "two")),
                                outerNodeEntry(2, innerNode("three", "four")))));

                final MockDataChangeListener listener = new MockDataChangeListener(0);
                final ActorRef dclActor = actorFactory.createActor(DataChangeListener.props(listener),
                        "testInitialChangeListenerEventWhenNotInitiallyLeader-DataChangeListener");
                final DataChangeListenerSupport support = new DataChangeListenerSupport(shard);
                support.onMessage(new RegisterChangeListener(
                        OUTER_LIST_PATH.node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME), dclActor,
                        DataChangeScope.ONE, false), false, true);

                listener.expectNoMoreChanges("Unexpected initial change event");
                listener.reset(1);

                support.onLeadershipChange(true, true);

                listener.waitForChangeEvents();
                listener.verifyCreatedData(0, innerEntryPath(1, "one"));
                listener.verifyCreatedData(0, innerEntryPath(1, "two"));
                listener.verifyCreatedData(0, innerEntryPath(2, "three"));
                listener.verifyCreatedData(0, innerEntryPath(2, "four"));
            }
        };

    }

}
