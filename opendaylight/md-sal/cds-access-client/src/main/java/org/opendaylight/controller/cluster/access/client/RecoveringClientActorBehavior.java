/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotOffer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transient behavior handling messages during initial actor recovery.
 */
final class RecoveringClientActorBehavior extends AbstractClientActorBehavior<InitialClientActorContext> {
    private record RecoveredState(ClientIdentifier clientId, boolean tombstone) {
        RecoveredState {
            requireNonNull(clientId);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RecoveringClientActorBehavior.class);

    /*
     * Base for the property name which overrides the initial generation when we fail to find anything from persistence.
     * The actual property name has the frontend type name appended.
     */
    private static final String GENERATION_OVERRIDE_PROP_BASE =
            "org.opendaylight.controller.cluster.access.client.initial.generation.";
    private static final String PROP_MEMBER_NAME = "member-name";
    private static final String PROP_CLIENT_TYPE = "client-type";
    private static final String PROP_GENERATION = "generation";

    private final FrontendIdentifier currentFrontend;
    private final Path filePath;

    private RecoveredState recoveredState;

    RecoveringClientActorBehavior(final Path statePath, final AbstractClientActor actor,
            final FrontendIdentifier frontendId) {
        super(new InitialClientActorContext(actor, frontendId.toPersistentId()));
        filePath = statePath.resolve("org.opendaylight.controller.cluster.access.client-" + frontendId.getMemberName()
            + "-" + frontendId.getClientType() + ".properties");
        currentFrontend = frontendId;
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveCommand(final Object command) {
        throw new IllegalStateException("Frontend is recovering");
    }

    @Override
    AbstractClientActorBehavior<?> onReceiveRecover(final Object recover) {
        return switch (recover) {
            case SnapshotOffer msg -> onSnapshotOffer(msg);
            case RecoveryCompleted msg -> onRecoveryCompleted();
            default -> {
                LOG.warn("{}: ignoring recovery message {}", persistenceId(), recover);
                yield this;
            }
        };
    }

    private RecoveringClientActorBehavior onSnapshotOffer(final SnapshotOffer msg) {
        final var snapshot = msg.snapshot();
        recoveredState = switch (snapshot) {
            case ClientIdentifier clientId -> new RecoveredState(clientId, false);
            case PersistenceTombstone tombstone ->  new RecoveredState(tombstone.clientId(), true);
            default -> throw new IllegalStateException("Unsupported snapshot " + snapshot);
        };

        LOG.debug("{}: recovered {}", persistenceId(), recoveredState);
        return this;
    }

    private AbstractClientActorBehavior<?> onRecoveryCompleted() {
        final var local = recoveredState;
        if (local == null) {
            return startWithoutRecovered();
        }

        // Make sure recovered ClientIdentifier matches our identifier
        final var clientId = local.clientId;
        if (!isValidIdentifier(clientId)) {
            return null;
        }

        return local.tombstone ? startWithTombstone(clientId) : startWithoutTombstone(clientId);
    }

    // We have recovered a PersistenceTombstone: we will not be touching persistence again
    private ClientActorBehavior<?> startWithTombstone(final ClientIdentifier fromPersistence) {
        final var fromFile = loadStateFile(filePath);
        final long lastGeneration;
        if (fromFile != null) {
            // Make sure loaded ClientIdentifier matches our identifier
            if (!isValidIdentifier(fromFile)) {
                return null;
            }

            // validate that file has equal-or-higher generation than tombstone
            final var fileGen = fromFile.getGeneration();
            if (Long.compareUnsigned(fromPersistence.getGeneration(), fileGen) <= 0) {
                LOG.error("{}: tombstone {} is newer than {} from {}. Shutting down.", persistenceId(), fromPersistence,
                    fromFile, filePath);
                return null;
            }

            lastGeneration = fileGen;
        } else {
            LOG.warn("{}: missing file {}, attempting to recover from tombstone {}", persistenceId(), filePath,
                fromPersistence);
            lastGeneration = fromPersistence.getGeneration();
        }

        final var clientId = nextClientId(lastGeneration);
        if (clientId == null) {
            return null;
        }

        // Write state file and transition directly to user behaviour
        createStateFile(clientId);
        return context().createBehavior(clientId);
    }

    // We have recovered a ClientIdentifier: we need to finish the transition to state file
    private SavingClientActorBehavior startWithoutTombstone(final ClientIdentifier fromPersistence) {
        final var fromFile = loadStateFile(filePath);

        throw new UnsupportedOperationException();
    }

    // There is nothing in persistence: we either have a fresh start or a wiped persistence
    private SavingClientActorBehavior startWithoutRecovered() {
        final var fromFile = loadStateFile(filePath);
        if (fromFile != null) {
            return isValidIdentifier(fromFile) ? saveTombstone(fromFile.getGeneration()) : null;
        }
        return saveTombstone(ClientIdentifier.create(currentFrontend, initialGeneration()));
    }

    private SavingClientActorBehavior saveTombstone(final long lastGeneration) {
        final var clientId = nextClientId(lastGeneration);
        return clientId == null ? null : saveTombstone(clientId);
    }

    private SavingClientActorBehavior saveTombstone(final ClientIdentifier clientId) {
        createStateFile(clientId);
        LOG.debug("{}: persisting new identifier {}", persistenceId(), clientId);
        context().saveSnapshot(clientId);
        return new SavingClientActorBehavior(context(), clientId);
    }

    private @Nullable ClientIdentifier nextClientId(final long lastGeneration) {
        // increment generation and refuse to wraparound
        final var nextGeneration = lastGeneration + 1;
        if (nextGeneration == 0) {
            LOG.error("{}: generation counter exhausted for {}. Shutting down.", persistenceId(), currentFrontend);
            return null;
        }
        return ClientIdentifier.create(currentFrontend, nextGeneration);
    }

    private boolean isValidIdentifier(final ClientIdentifier clientId) {
        final var frontedId = clientId.getFrontendId();
        final var matches = currentFrontend.equals(frontedId);
        if (!matches) {
            LOG.error("{}: Mismatched frontend identifier, shutting down. Current: {} Saved: {}", persistenceId(),
                currentFrontend, frontedId);
        }
        return matches;
    }

    private void createStateFile(final ClientIdentifier clientId) {
        LOG.debug("{}: saving new identifier {} to {}", persistenceId(), clientId, filePath);
        createStateFile(filePath, clientId);
    }

    private static void createStateFile(final Path path, final ClientIdentifier clientId) {
        final var props = new Properties();
        final var frontendId = clientId.getFrontendId();
        props.setProperty(PROP_MEMBER_NAME, frontendId.getMemberName().getName());
        props.setProperty(PROP_CLIENT_TYPE, frontendId.getClientType().getName());
        props.setProperty(PROP_GENERATION, Long.toUnsignedString(clientId.getGeneration()));

        createStateFile(path, props);
    }

    private static void createStateFile(final Path path, final Properties props) {
        final var parent = path.getParent();

        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create directory structure to " + parent, e);
        }

        final Path temp;
        try {
            temp = Files.createTempFile(parent, "cds-id", null);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create temporary file", e);
        }

        try {
            try (var os = Files.newOutputStream(temp,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC)) {
                props.store(os, "Critical persistent state. Do not touch unless you know what you are doing!");
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write " + temp, e);
            }

            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to instate " + path, e);
            }
        } finally {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException e) {
                LOG.warn("Failed to delete " + temp, e);
            }
        }
    }

    private long initialGeneration() {
        final String propName = GENERATION_OVERRIDE_PROP_BASE + currentFrontend.getClientType().getName();
        final String propValue = System.getProperty(propName);
        if (propValue == null) {
            LOG.debug("{}: no initial generation override, starting from 0", persistenceId());
            return 0;
        }

        final long ret;
        try {
            ret = Long.parseUnsignedLong(propValue);
        } catch (NumberFormatException e) {
            LOG.warn("{}: failed to parse initial generation override '{}', starting from 0", persistenceId(),
                propValue, e);
            return 0;
        }

        LOG.info("{}: initial generation set to {}", persistenceId(), ret);
        return ret;
    }

    private static @Nullable ClientIdentifier loadStateFile(final Path path) {
        if (!Files.exists(path)) {
            return null;
        }

        final var props = new Properties();
        try (var is = Files.newInputStream(path)) {
            props.load(is);
        } catch (IllegalArgumentException | IOException e) {
            throw new IllegalStateException("Failed to load " + path, e);
        }

        final var frontendId = FrontendIdentifier.create(
            MemberName.forName(requireProp(props, PROP_MEMBER_NAME, path)),
            FrontendType.forName(requireProp(props, PROP_CLIENT_TYPE, path)));

        final var generationStr = requireProp(props, PROP_GENERATION, path);
        final long generation;
        try {
            generation = Long.parseUnsignedLong(generationStr);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(path + " contains illegal generation " + generationStr, e);
        }

        return ClientIdentifier.create(frontendId, generation);
    }

    private static String requireProp(final Properties props, final String key, final Path path) {
        final var value = props.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(path + " is missing property " + key);
        }
        return value;
    }
}
