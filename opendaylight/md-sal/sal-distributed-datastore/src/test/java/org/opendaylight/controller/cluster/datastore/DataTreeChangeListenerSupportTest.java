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
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Throwables;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistrationReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.MockDataTreeChangeListener;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

/**
 * Unit tests for DataTreeChangeListenerSupport.
 *
 * @author Thomas Pantelis
 */
public class DataTreeChangeListenerSupportTest extends AbstractShardTest {
    private Shard shard;
    private TestActorRef<Shard> shardActor;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        createShard();
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        actorFactory.close();
    }

    @Test
    public void testChangeListenerWithNoInitialData() throws Exception {
        MockDataTreeChangeListener listener = registerChangeListener(TEST_PATH, 0).getKey();

        listener.expectNoMoreChanges("Unexpected initial change event");
    }

    @Test
    public void testInitialChangeListenerEventWithContainerPath() throws Exception {
        writeToStore(shard.getDataStore(), TEST_PATH, ImmutableNodes.containerNode(TEST_QNAME));

        Entry<MockDataTreeChangeListener, ActorSelection> entry = registerChangeListener(TEST_PATH, 1);
        MockDataTreeChangeListener listener = entry.getKey();

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(TEST_PATH);

        listener.reset(1);

        writeToStore(shard.getDataStore(), TEST_PATH, ImmutableNodes.containerNode(TEST_QNAME));
        listener.waitForChangeEvents();
        listener.verifyNotifiedData(TEST_PATH);

        listener.reset(1);
        JavaTestKit kit = new JavaTestKit(getSystem());
        entry.getValue().tell(CloseDataTreeNotificationListenerRegistration.getInstance(), kit.getRef());
        kit.expectMsgClass(JavaTestKit.duration("5 seconds"), CloseDataTreeNotificationListenerRegistrationReply.class);

        writeToStore(shard.getDataStore(), TEST_PATH, ImmutableNodes.containerNode(TEST_QNAME));
        listener.verifyNoNotifiedData(TEST_PATH);
    }

    @Test
    public void testInitialChangeListenerEventWithListPath() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));

        MockDataTreeChangeListener listener = registerChangeListener(OUTER_LIST_PATH, 1).getKey();

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(OUTER_LIST_PATH);
    }

    @Test
    public void testInitialChangeListenerEventWithWildcardedListPath() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));

        MockDataTreeChangeListener listener =
                registerChangeListener(OUTER_LIST_PATH.node(OUTER_LIST_QNAME), 1).getKey();

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(outerEntryPath(1), outerEntryPath(2));
    }

    @Test
    public void testInitialChangeListenerEventWithNestedWildcardedListsPath() throws Exception {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(outerNode(
                outerNodeEntry(1, innerNode("one", "two")), outerNodeEntry(2, innerNode("three", "four")))));

        MockDataTreeChangeListener listener = registerChangeListener(
                OUTER_LIST_PATH.node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME), 1).getKey();

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(innerEntryPath(1, "one"), innerEntryPath(1, "two"), innerEntryPath(2, "three"),
                innerEntryPath(2, "four"));

        // Register for a specific outer list entry

        MockDataTreeChangeListener listener2 = registerChangeListener(
                OUTER_LIST_PATH.node(outerEntryKey(1)).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME), 1).getKey();

        listener2.waitForChangeEvents();
        listener2.verifyNotifiedData(innerEntryPath(1, "one"), innerEntryPath(1, "two"));
        listener2.verifyNoNotifiedData(innerEntryPath(2, "three"), innerEntryPath(2, "four"));

        listener.reset(1);
        listener2.reset(1);

        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(outerNode(
                outerNodeEntry(1, innerNode("three")))));

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(innerEntryPath(1, "three"));

        listener2.waitForChangeEvents();
        listener2.verifyNotifiedData(innerEntryPath(1, "three"));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private Entry<MockDataTreeChangeListener, ActorSelection> registerChangeListener(final YangInstanceIdentifier path,
            final int expectedEvents) {
        MockDataTreeChangeListener listener = new MockDataTreeChangeListener(expectedEvents);
        ActorRef dclActor = actorFactory.createActor(DataTreeChangeListenerActor.props(listener, TestModel.TEST_PATH));

        try {
            RegisterDataTreeChangeListenerReply reply = (RegisterDataTreeChangeListenerReply)
                Await.result(Patterns.ask(shardActor, new RegisterDataTreeChangeListener(path, dclActor, false),
                    new Timeout(5, TimeUnit.SECONDS)), Duration.create(5, TimeUnit.SECONDS));
            return new SimpleEntry<>(listener, getSystem().actorSelection(reply.getListenerRegistrationPath()));

        } catch (Exception e) {
            Throwables.propagate(e);
            return null;
        }
    }

    private void createShard() {
        shardActor = actorFactory.createTestActor(newShardProps());
        ShardTestKit.waitUntilLeader(shardActor);
        shard = shardActor.underlyingActor();
    }
}
