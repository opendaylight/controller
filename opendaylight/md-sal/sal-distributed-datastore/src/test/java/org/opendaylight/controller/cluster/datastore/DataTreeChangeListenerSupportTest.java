/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.EMPTY_TEST;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.INNER_LIST_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.OUTER_LIST_PATH;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.OUTER_LIST_QNAME;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.TEST_PATH;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerEntryPath;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.innerNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerEntry;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerEntryKey;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerEntryPath;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.outerNode;
import static org.opendaylight.controller.md.cluster.datastore.model.TestModel.testNodeWithOuter;

import java.time.Duration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistrationReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.utils.MockDataTreeChangeListener;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

/**
 * Unit tests for DataTreeChangeListenerSupport.
 *
 * @author Thomas Pantelis
 */
public class DataTreeChangeListenerSupportTest extends AbstractShardTest {
    private Shard shard;
    private TestActorRef<Shard> shardActor;

    @Before
    public void setUp() throws Exception {
        createShard();
    }

    @Override
    @After
    public void tearDown() {
        super.tearDown();
        actorFactory.close();
    }

    @Test
    public void testChangeListenerWithNoInitialData() {
        final var listener = registerChangeListener(TEST_PATH, 0).getKey();
        listener.expectNoMoreChanges("Unexpected initial change event");
    }

    @Test
    public void testInitialChangeListenerEventWithContainerPath() throws DataValidationFailedException {
        writeToStore(shard.getDataStore(), TEST_PATH, EMPTY_TEST);

        final var entry = registerChangeListener(TEST_PATH, 1);
        final var listener = entry.getKey();

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(TEST_PATH);

        listener.reset(1);

        writeToStore(shard.getDataStore(), TEST_PATH, EMPTY_TEST);
        listener.waitForChangeEvents();
        listener.verifyNotifiedData(TEST_PATH);

        listener.reset(1);
        final var kit = new TestKit(getSystem());
        entry.getValue().tell(CloseDataTreeNotificationListenerRegistration.INSTANCE, kit.getRef());
        kit.expectMsgClass(Duration.ofSeconds(5), CloseDataTreeNotificationListenerRegistrationReply.class);

        writeToStore(shard.getDataStore(), TEST_PATH, EMPTY_TEST);
        listener.verifyNoNotifiedData(TEST_PATH);
    }

    @Test
    public void testInitialChangeListenerEventWithListPath() throws DataValidationFailedException {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));

        final var listener = registerChangeListener(OUTER_LIST_PATH, 1).getKey();

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(OUTER_LIST_PATH);
    }

    @Test
    public void testInitialChangeListenerEventWithWildcardedListPath() throws DataValidationFailedException {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(1, 2));

        final var listener = registerChangeListener(OUTER_LIST_PATH.node(OUTER_LIST_QNAME), 1).getKey();

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(outerEntryPath(1), outerEntryPath(2));
    }

    @Test
    public void testInitialChangeListenerEventWithNestedWildcardedListsPath() throws DataValidationFailedException {
        mergeToStore(shard.getDataStore(), TEST_PATH, testNodeWithOuter(outerNode(
                outerEntry(1, innerNode("one", "two")), outerEntry(2, innerNode("three", "four")))));

        final var listener = registerChangeListener(
                OUTER_LIST_PATH.node(OUTER_LIST_QNAME).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME), 1).getKey();

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(innerEntryPath(1, "one"), innerEntryPath(1, "two"), innerEntryPath(2, "three"),
                innerEntryPath(2, "four"));

        // Register for a specific outer list entry

        final var listener2 = registerChangeListener(
                OUTER_LIST_PATH.node(outerEntryKey(1)).node(INNER_LIST_QNAME).node(INNER_LIST_QNAME), 1).getKey();

        listener2.waitForChangeEvents();
        listener2.verifyNotifiedData(innerEntryPath(1, "one"), innerEntryPath(1, "two"));
        listener2.verifyNoNotifiedData(innerEntryPath(2, "three"), innerEntryPath(2, "four"));

        listener.reset(1);
        listener2.reset(1);

        mergeToStore(shard.getDataStore(), TEST_PATH,
            testNodeWithOuter(outerNode(outerEntry(1, innerNode("three")))));

        listener.waitForChangeEvents();
        listener.verifyNotifiedData(innerEntryPath(1, "three"));

        listener2.waitForChangeEvents();
        listener2.verifyNotifiedData(innerEntryPath(1, "three"));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private Entry<MockDataTreeChangeListener, ActorSelection> registerChangeListener(final YangInstanceIdentifier path,
            final int expectedEvents) {
        final var listener = new MockDataTreeChangeListener(expectedEvents);
        final var dclActor = actorFactory.createActor(DataTreeChangeListenerActor.props(listener, TestModel.TEST_PATH));

        RegisterDataTreeNotificationListenerReply reply;
        try {
            reply = (RegisterDataTreeNotificationListenerReply)
                    Await.result(Patterns.ask(shardActor, new RegisterDataTreeChangeListener(path, dclActor, false),
                        new Timeout(5, TimeUnit.SECONDS)), FiniteDuration.create(5, TimeUnit.SECONDS));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return Map.entry(listener, getSystem().actorSelection(reply.getListenerRegistrationPath()));
    }

    private void createShard() {
        shardActor = actorFactory.createTestActor(newShardProps());
        ShardTestKit.waitUntilLeader(shardActor);
        shard = shardActor.underlyingActor();
    }

    private static void mergeToStore(final ShardDataTree store, final YangInstanceIdentifier id,
            final NormalizedNode node) throws DataValidationFailedException {
        final var modification = store.getDataTree().takeSnapshot().newModification();
        modification.merge(id, node);
        store.notifyListeners(commitTransaction(store.getDataTree(), modification));
    }

    private static void writeToStore(final ShardDataTree store, final YangInstanceIdentifier id,
            final NormalizedNode node) throws DataValidationFailedException {
        final var modification = store.getDataTree().takeSnapshot().newModification();
        modification.write(id, node);
        store.notifyListeners(commitTransaction(store.getDataTree(), modification));
    }
}
