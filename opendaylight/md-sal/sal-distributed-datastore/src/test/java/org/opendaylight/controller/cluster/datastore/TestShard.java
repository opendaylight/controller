/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.opendaylight.controller.cluster.datastore.messages.RequestFrontendMetadata;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendShardDataTreeSnapshotMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestShard extends Shard {

    private static final Logger LOG = LoggerFactory.getLogger(TestShard.class);

    private final Map<Class<?>, Predicate<?>> dropMessages = new ConcurrentHashMap<>();

    protected TestShard(Builder builder) {
        super(builder);
    }

    @Override
    protected void handleNonRaftCommand(Object message) {
        if (message instanceof RequestFrontendMetadata) {
            handleRequestFrontendMetadata(sender());
            return;
        }
        super.handleNonRaftCommand(message);
    }

    @SuppressWarnings({ "checkstyle:IllegalCatch" })
    @Override
    protected void handleCommand(Object message) {
        if (message instanceof StartDropMessages) {
            startDropMessages(((StartDropMessages) message).getMsgClass());
        }
        if (message instanceof StopDropMessages) {
            stopDropMessages(((StopDropMessages) message).getMsgClass());
        }

        Predicate drop = dropMessages.get(message.getClass());
        if (drop == null || !drop.test(message)) {
            super.handleCommand(message);
        }
    }

    private void handleRequestFrontendMetadata(final ActorRef respondTo) {
        FrontendShardDataTreeSnapshotMetadata metadataSnapshot = frontendMetadata.toSnapshot();
        respondTo.tell(metadataSnapshot, self());
    }

    public void startDropMessages(final Class<?> msgClass) {
        dropMessages.put(msgClass, msg -> true);
    }

    <T> void startDropMessages(final Class<T> msgClass, final Predicate<T> filter) {
        dropMessages.put(msgClass, filter);
    }

    public void stopDropMessages(final Class<?> msgClass) {
        dropMessages.remove(msgClass);
    }

    public static TestShard.Builder builder() {
        return new TestShard.Builder();
    }

    public static class Builder extends Shard.Builder {

        Builder() {
            super();
        }

        @Override
        public Props props() {
            sealed = true;
            verify();
            return Props.create(TestShard.class, this);
        }
    }

    private abstract static class DropMessages {
        private Class<?> msgClass;

        DropMessages(final Class<?> msgClass) {
            this.msgClass = msgClass;
        }

        Class<?> getMsgClass() {
            return msgClass;
        }
    }


    public static class StartDropMessages extends DropMessages {
        public StartDropMessages(Class<?> msgClass) {
            super(msgClass);
        }
    }

    public static class StopDropMessages extends DropMessages {
        public StopDropMessages(Class<?> msgClass) {
            super(msgClass);
        }
    }


}

