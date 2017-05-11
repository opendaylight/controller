/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.serialization.JavaSerializer;
import akka.testkit.TestProbe;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

public class ConnectClientSuccessTest extends AbstractRequestSuccessTest<ConnectClientSuccess> {

    private static final DataTree TREE = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    private static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();
    private static final ActorSelection ACTOR_SELECTION =  ActorSelection.apply(ACTOR_REF, "foo");
    private static final List<ActorSelection> ALTERNATES = ImmutableList.of(ACTOR_SELECTION);
    private static final int MAX_MESSAGES = 10;
    private static final ConnectClientSuccess OBJECT = new ConnectClientSuccess(
            CLIENT_IDENTIFIER, 0, ACTOR_REF, ALTERNATES, TREE, MAX_MESSAGES);

    @Override
    protected ConnectClientSuccess object() {
        return OBJECT;
    }

    @Before
    public void setUp() {
        JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) SYSTEM);
    }

    @Test
    public void testGetAlternates() {
        final Collection<ActorSelection> alternates = OBJECT.getAlternates();
        Assert.assertArrayEquals(ALTERNATES.toArray(), alternates.toArray());
    }

    @Test
    public void testGetBackend() {
        final ActorRef actorRef = OBJECT.getBackend();
        Assert.assertEquals(ACTOR_REF, actorRef);
    }

    @Test
    public void testGetDataTree() {
        final DataTree tree = OBJECT.getDataTree().get();
        Assert.assertEquals(TREE, tree);
    }

    @Test
    public void testGetMaxMessages() {
        final int maxMessages = OBJECT.getMaxMessages();
        Assert.assertEquals(MAX_MESSAGES, maxMessages);
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final ConnectClientSuccess clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }

    @Test
    public void addToStringAttributes() {
        // Just verify it doesn't throw an exception.
        OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT));
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        Assert.assertTrue(deserialize instanceof ConnectClientSuccess);
        Assert.assertEquals(OBJECT.getAlternates().size(), ((ConnectClientSuccess) deserialize).getAlternates().size());
        Assert.assertEquals(OBJECT.getBackend(), ((ConnectClientSuccess) deserialize).getBackend());
        Assert.assertEquals(Optional.empty(), ((ConnectClientSuccess) deserialize).getDataTree());
        Assert.assertEquals(OBJECT.getMaxMessages(), ((ConnectClientSuccess) deserialize).getMaxMessages());
    }
}
