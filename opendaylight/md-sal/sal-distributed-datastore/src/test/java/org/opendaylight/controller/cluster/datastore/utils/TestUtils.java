/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import java.util.List;
import org.junit.Assert;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class TestUtils {

    public static void assertFirstSentMessage(final ActorSystem actorSystem, final ActorRef actorRef, final Class<?> clazz){
        ActorContext testContext = new ActorContext(actorSystem, actorSystem.actorOf(
            Props.create(DoNothingActor.class)), new MockClusterWrapper(), new MockConfiguration());
        Object messages = testContext
            .executeOperation(actorRef, MessageCollectorActor.GET_ALL_MESSAGES);

        Assert.assertNotNull(messages);

        Assert.assertTrue(messages instanceof List);

        List<?> listMessages = (List<?>) messages;

        Assert.assertEquals(1, listMessages.size());

        Assert.assertTrue(listMessages.get(0).getClass().equals(clazz));
    }
}
