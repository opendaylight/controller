/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class TestShard extends Shard {
    public static class Builder extends Shard.Builder {
        Builder() {
            super(TestShard.class);
        }
    }

    // Message to request FrontendMetadata
    public static final class RequestFrontendMetadata {

    }

    private abstract static sealed class DropMessages<T> {
        private final Class<T> msgClass;

        DropMessages(final Class<T> msgClass) {
            this.msgClass = requireNonNull(msgClass);
        }

        final Class<T> getMsgClass() {
            return msgClass;
        }
    }

    public static final class StartDropMessages<T> extends DropMessages<T> {
        public StartDropMessages(final Class<T> msgClass) {
            super(msgClass);
        }
    }

    public static final class StopDropMessages<T> extends DropMessages<T> {
        public StopDropMessages(final Class<T> msgClass) {
            super(msgClass);
        }
    }

    private final Map<Class<?>, Predicate<?>> dropMessages = new ConcurrentHashMap<>();

    protected TestShard(final Path stateDir, final AbstractBuilder<?, ?> builder) {
        super(stateDir, builder);
    }

    @Override
    protected void handleNonRaftCommand(final Object message) {
        if (message instanceof RequestFrontendMetadata) {
            getSender().tell(frontendMetadata.toSnapshot(), self());
        } else {
            super.handleNonRaftCommand(message);
        }
    }

    @Override
    protected void handleCommand(final Object message) {
        switch (message) {
            case StartDropMessages<?> msg -> startDropMessages(msg.getMsgClass());
            case StopDropMessages<?> msg -> stopDropMessages(msg.getMsgClass());
            default -> dropOrHandle(message);
        }
    }

    private void dropOrHandle(final Object message) {
        @SuppressWarnings("unchecked")
        final var drop = (Predicate<Object>) dropMessages.get(message.getClass());
        if (drop == null || !drop.test(message)) {
            super.handleCommand(message);
        }
    }

    private void startDropMessages(final Class<?> msgClass) {
        // TODO: use _ when we have Java 22+
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
}
