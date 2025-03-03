/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.serialization.JavaSerializer;
import org.apache.pekko.testkit.TestProbe;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;

public class ConnectClientSuccessTest extends AbstractRequestSuccessTest<ConnectClientSuccess> {
    private static final DataTree TREE = new InMemoryDataTreeFactory().create(
        DataTreeConfiguration.DEFAULT_OPERATIONAL);
    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    private static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();
    private static final ActorSelection ACTOR_SELECTION =  ActorSelection.apply(ACTOR_REF, "foo");
    private static final List<ActorSelection> ALTERNATES = ImmutableList.of(ACTOR_SELECTION);
    private static final int MAX_MESSAGES = 10;
    private static final ConnectClientSuccess OBJECT = new ConnectClientSuccess(CLIENT_IDENTIFIER, 0, ACTOR_REF,
        ALTERNATES, TREE, MAX_MESSAGES);

    public ConnectClientSuccessTest() {
        super(OBJECT, 147 + ACTOR_REF.path().toSerializationFormat().length());
    }

    @Before
    public void setUp() {
        JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) SYSTEM);
    }

    @Test
    public void testGetAlternates() {
        final var alternates = OBJECT.getAlternates();
        assertArrayEquals(ALTERNATES.toArray(), alternates.toArray());
    }

    @Test
    public void testGetBackend() {
        final ActorRef actorRef = OBJECT.getBackend();
        assertEquals(ACTOR_REF, actorRef);
    }

    @Test
    public void testGetDataTree() {
        final ReadOnlyDataTree tree = OBJECT.getDataTree().orElseThrow();
        assertEquals(TREE, tree);
    }

    @Test
    public void testGetMaxMessages() {
        assertEquals(MAX_MESSAGES, OBJECT.getMaxMessages());
    }

    @Test
    public void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.TEST_FUTURE_VERSION);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getAlternates(), clone.getAlternates());
        assertEquals(OBJECT.getBackend(), clone.getBackend());
        assertEquals(OBJECT.getDataTree(), clone.getDataTree());
        assertEquals(OBJECT.getMaxMessages(), clone.getMaxMessages());
    }

    @Test
    public void addToStringAttributes() {
        // Just verify it doesn't throw an exception.
        OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT));
    }

    @Override
    protected void doAdditionalAssertions(final ConnectClientSuccess deserialize) {
        assertEquals(OBJECT.getAlternates().size(), deserialize.getAlternates().size());
        assertEquals(OBJECT.getBackend(), deserialize.getBackend());
        assertEquals(Optional.empty(), deserialize.getDataTree());
        assertEquals(OBJECT.getMaxMessages(), deserialize.getMaxMessages());
    }
}
