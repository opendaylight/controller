/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.INNER_LIST_QNAME;
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
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.utils.MockDataTreeChangeListener;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Unit tests for DataTreeChangeListenerSupport.
 *
 * @author Thomas Pantelis
 */
public class DataTreeChangeListenerSupportTest extends AbstractShardTest {
    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private Shard shard;
    private DataTreeChangeListenerSupport support;

    @Before
    public void setup() {
        shard = createShard();
        support = new DataTreeChangeListenerSupport(shard);
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        actorFactory.close();
    }

    @Test
    public void testChangeListenerWithNoInitialData() throws Exception {
        MockDataTreeChangeListener listener = registerChangeListener(TEST_PATH, 0, true);

        listener.expectNoMoreChanges("Unexpected initial change event");
    }

    @Test
    public void testInitialChangeListenerEventWithContainerPath() throws Exception {
        writeToStore(shard.getDataStore(), TEST_PATH, ImmutableNodes.containerNode(TEST_QNAME));

        MockDataTreeChangeListener listener = registerChangeListener(TEST_PATH, 1, true);

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(TEST_PATH);
    }

    @Test
    public void testInitialChangeListenerEventWithListPath() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));

        MockDataTreeChangeListener listener = registerChangeListener(OUTER_LIST_PATH, 1, true);

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(OUTER_LIST_PATH);
    }

    @Test
    public void testInitialChangeListenerEventWithWildcardedListPath() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));

        MockDataTreeChangeListener listener = registerChangeListener(OUTER_LIST_PATH.node(OUTER_LIST_QNAME), 2, true);

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(outerEntryPath(1), outerEntryPath(2));
    }

    @Test
    public void testInitialChangeListenerEventWithNestedWildcardedListsPath() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(outerNode(
                outerNodeEntry(1, innerNode("one", "two")), outerNodeEntry(2, innerNode("three", "four")))));

        MockDataTreeChangeListener listener = registerChangeListener(
                OUTER_LIST_PATH.node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME), 4, true);

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(innerEntryPath(1, "one"), innerEntryPath(1, "two"), innerEntryPath(2, "three"),
                innerEntryPath(2, "four"));

        // Register for a specific outer list entry

        MockDataTreeChangeListener listener2 = registerChangeListener(
                OUTER_LIST_PATH.node(outerEntryKey(1)).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME), 2, true);

        listener2.waitForChangeEvents();
        listener2.verifyNotifiedData(innerEntryPath(1, "one"), innerEntryPath(1, "two"));
        listener2.verifyNoNotifiedData(innerEntryPath(2, "three"), innerEntryPath(2, "four"));
    }

    private MockDataTreeChangeListener registerChangeListener(final YangInstanceIdentifier path,
            final int expectedEvents, final boolean isLeader) {
        MockDataTreeChangeListener listener = new MockDataTreeChangeListener(expectedEvents);
        ActorRef dclActor = actorFactory.createActor(DataTreeChangeListenerActor.props(listener));
        support.onMessage(new RegisterDataTreeChangeListener(path, dclActor, false), isLeader, true);
        return listener;
    }

    private Shard createShard() {
        TestActorRef<Shard> actor = actorFactory.createTestActor(newShardProps());
        ShardTestKit.waitUntilLeader(actor);
        return actor.underlyingActor();
    }
}
