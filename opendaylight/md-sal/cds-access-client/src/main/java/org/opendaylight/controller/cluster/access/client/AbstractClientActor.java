/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.SYNC;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend actor which takes care of persisting generations and creates an appropriate ClientIdentifier.
 */
public abstract class AbstractClientActor extends AbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientActor.class);
    private static final Path STATE_PATH = Path.of("state");
    private static final String PROP_MEMBER_NAME = "member-name";
    private static final String PROP_CLIENT_TYPE = "client-type";
    private static final String PROP_GENERATION = "generation";

    /*
     * Base for the property name which overrides the initial generation when we fail to find anything from persistence.
     * The actual property name has the frontend type name appended.
     */
    private static final String GENERATION_OVERRIDE_PROP_BASE =
            "org.opendaylight.controller.cluster.access.client.initial.generation.";

    private final @NonNull FrontendIdentifier currentFrontend;
    private final @NonNull String persistenceId;
    private final Path statePath;

    private ClientActorBehavior<?> currentBehavior;

    protected AbstractClientActor(final FrontendIdentifier frontendId) {
        this(STATE_PATH, frontendId);
    }

    @VisibleForTesting
    protected AbstractClientActor(final Path statePath, final FrontendIdentifier frontendId) {
        this.statePath = requireNonNull(statePath);
        currentFrontend = requireNonNull(frontendId);
        persistenceId = frontendId.toPersistentId();
    }

    protected final String persistenceId() {
        return persistenceId;
    }

    @Override
    public void preStart() throws IOException, RecoveryException {
        final var filePath = statePath
            .resolve("odl.cluster.client")
            .resolve(currentFrontend.getMemberName().getName())
            .resolve(currentFrontend.getClientType().getName() + ".properties");

        final long nextGeneration;
        final var lastClientId = loadStateFile(filePath);
        if (lastClientId != null) {
            // increment generation and refuse to wraparound
            nextGeneration = lastClientId.getGeneration() + 1;
            if (nextGeneration == 0) {
                throw new RecoveryException("Generation counter exhausted for %s", currentFrontend);
            }
        } else {
            nextGeneration = initialGeneration();
        }

        final var clientId = ClientIdentifier.create(currentFrontend, nextGeneration);
        LOG.debug("{}: saving new identifier {} to {}", persistenceId, clientId, filePath);
        createStateFile(filePath, clientId);

        currentBehavior = initialBehavior(new ClientActorContext(self(), persistenceId, getContext().system(),
            clientId, getClientActorConfig()));
    }

    @Override
    public void postStop() {
        if (currentBehavior != null) {
            currentBehavior.close();
        }
    }

    protected abstract ClientActorBehavior<?> initialBehavior(ClientActorContext context);

    protected abstract ClientActorConfig getClientActorConfig();

    @Override
    public final Receive createReceive() {
        return receiveBuilder().matchAny(this::onReceiveCommand).build();
    }

    private void onReceiveCommand(final Object command) {
        if (command == null) {
            LOG.debug("{}: ignoring null command", persistenceId);
            return;
        }

        if (currentBehavior != null) {
            switchBehavior(currentBehavior.onReceiveCommand(command));
        } else {
            LOG.debug("{}: shutting down, ignoring command {}", persistenceId, command);
        }
    }

    private void switchBehavior(final ClientActorBehavior<?> nextBehavior) {
        if (!currentBehavior.equals(nextBehavior)) {
            if (nextBehavior == null) {
                LOG.debug("{}: shutting down", persistenceId);
                self().tell(PoisonPill.getInstance(), ActorRef.noSender());
            } else {
                LOG.debug("{}: switched from {} to {}", persistenceId, currentBehavior, nextBehavior);
            }

            currentBehavior.close();
            currentBehavior = nextBehavior;
        }
    }

    private @Nullable ClientIdentifier loadStateFile(final Path filePath) throws IOException, RecoveryException {
        if (!Files.exists(filePath)) {
            return null;
        }

        final var props = new Properties();
        try (var is = Files.newInputStream(filePath)) {
            props.load(is);
        } catch (IllegalArgumentException e) {
            throw new RecoveryException(e, "Failed to load %s", filePath);
        }

        final var frontendId = FrontendIdentifier.create(
            MemberName.forName(requireProp(props, PROP_MEMBER_NAME, filePath)),
            FrontendType.forName(requireProp(props, PROP_CLIENT_TYPE, filePath)));
        if (!currentFrontend.equals(frontendId)) {
            throw new RecoveryException("Mismatched frontend identifier: current: %s saved: %s", currentFrontend,
                frontendId);
        }

        final var generationStr = requireProp(props, PROP_GENERATION, filePath);
        final long generation;
        try {
            generation = Long.parseUnsignedLong(generationStr);
        } catch (NumberFormatException e) {
            throw new RecoveryException(e, "%s contains illegal generation %s", filePath, generationStr);
        }

        return ClientIdentifier.create(frontendId, generation);
    }

    private long initialGeneration() {
        final String propName = GENERATION_OVERRIDE_PROP_BASE + currentFrontend.getClientType().getName();
        final String propValue = System.getProperty(propName);
        if (propValue == null) {
            LOG.debug("{}: no initial generation override, starting from 0", persistenceId);
            return 0;
        }

        final long ret;
        try {
            ret = Long.parseUnsignedLong(propValue);
        } catch (NumberFormatException e) {
            LOG.warn("{}: failed to parse initial generation override '{}', starting from 0", persistenceId, propValue,
                e);
            return 0;
        }

        LOG.info("{}: initial generation set to {}", persistenceId, ret);
        return ret;
    }

    private static void createStateFile(final Path path, final ClientIdentifier clientId) throws IOException {
        final var props = new Properties();
        final var frontendId = clientId.getFrontendId();
        props.setProperty(PROP_MEMBER_NAME, frontendId.getMemberName().getName());
        props.setProperty(PROP_CLIENT_TYPE, frontendId.getClientType().getName());
        props.setProperty(PROP_GENERATION, Long.toUnsignedString(clientId.getGeneration()));

        createStateFile(path, props);
    }

    private static void createStateFile(final Path path, final Properties props) throws IOException {
        final var parent = path.getParent();
        Files.createDirectories(parent);

        final var temp = Files.createTempFile(parent, "cds-id", null);

        try {
            try (var os = Files.newOutputStream(temp, WRITE, TRUNCATE_EXISTING, SYNC)) {
                props.store(os, "Critical persistent state. Do not touch unless you know what you are doing!");
            }

            Files.move(temp, path, ATOMIC_MOVE, REPLACE_EXISTING);
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException e) {
                LOG.warn("Failed to delete {}", temp, e);
            }
        }
    }

    private static String requireProp(final Properties props, final String key, final Path path)
            throws RecoveryException {
        final var value = props.getProperty(key);
        if (value == null) {
            throw new RecoveryException("%s is missing property %s", path, key);
        }
        return value;
    }
}
