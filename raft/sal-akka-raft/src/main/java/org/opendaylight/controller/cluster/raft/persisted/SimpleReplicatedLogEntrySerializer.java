/*
 * Copyright (c) 2018 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.serialization.JSerializer;
import org.apache.pekko.util.ClassLoaderObjectInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized serializer for {@link SimpleReplicatedLogEntry} that optimizes serialization.
 *
 * @author Thomas Pantelis
 */
public class SimpleReplicatedLogEntrySerializer extends JSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleReplicatedLogEntrySerializer.class);

    private final ExtendedActorSystem system;

    public SimpleReplicatedLogEntrySerializer(final ExtendedActorSystem system) {
        this.system = requireNonNull(system);
    }

    @Override
    public int identifier() {
        return 97439500;
    }

    @Override
    public boolean includeManifest() {
        return false;
    }

    @Override
    public byte[] toBinary(final Object obj) {
        if (!(obj instanceof SimpleReplicatedLogEntry entry)) {
            throw new IllegalArgumentException("Unsupported object type " + obj.getClass());
        }

        final int estimatedSize = entry.serializedSize();
        final byte[] bytes;
        try (var baos = new ByteArrayOutputStream(estimatedSize)) {
            try (var out = new ObjectOutputStream(baos)) {
                out.writeObject(entry);
            }
            bytes = baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (LOG.isDebugEnabled()) {
            final var payload = entry.command();
            LOG.debug("Estimated serialized size {}, data size {} for payload: {}. Actual serialized size: {}",
                estimatedSize, payload.size(), payload, bytes.length);
        }

        return bytes;
    }

    @Override
    public Object fromBinaryJava(final byte[] bytes, final Class<?> manifest) {
        try (ClassLoaderObjectInputStream is = new ClassLoaderObjectInputStream(system.dynamicAccess().classLoader(),
                new ByteArrayInputStream(bytes))) {
            return is.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize object", e);
        }
    }
}
