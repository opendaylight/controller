/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeStreamVersion;
import org.opendaylight.yangtools.concepts.WritableObject;

/**
 * Enumeration of all ABI versions supported by this implementation of persistence. An ABI version has to be bumped
 * whenever:
 * <ul>
 * <li>a new event is defined</li>
 * <li>serialization format is changed</li>
 * </ul>
 *
 * <p>
 * This version effectively defines the protocol version between actors participating on a particular shard. A shard
 * participant instance should oppose RAFT candidates which produce persistence of an unsupported version. If a follower
 * encounters an unsupported version it must not become fully-operational, as it does not have an accurate view
 * of shard state.
 *
 * @author Robert Varga
 */
@Beta
public enum PayloadVersion implements WritableObject {
    // NOTE: enumeration values need to be sorted in ascending order of their version to keep Comparable working

    /**
     * Version which is older than any other version. This version exists purely for testing purposes.
     */
    @VisibleForTesting
    TEST_PAST_VERSION(0) {
        @Override
        public NormalizedNodeStreamVersion getStreamVersion() {
            throw new UnsupportedOperationException();
        }
    },

    /**
     * Initial ABI version, as shipped with Boron Simultaneous release.
     */
    // We seed the initial version to be the same as DataStoreVersions.BORON_VERSION for compatibility reasons.
    BORON(5) {
        @Override
        public NormalizedNodeStreamVersion getStreamVersion() {
            return NormalizedNodeStreamVersion.LITHIUM;
        }
    },

    /**
     * Revised payload version. Payloads remain the same as {@link #BORON}, but messages bearing QNames in any shape
     * are using {@link NormalizedNodeStreamVersion#NEON_SR2}, which improves encoding.
     */
    NEON_SR2(6) {
        @Override
        public NormalizedNodeStreamVersion getStreamVersion() {
            return NormalizedNodeStreamVersion.NEON_SR2;
        }
    },

    /**
     * Revised payload version. Payloads remain the same as {@link #NEON_SR2}, but messages bearing QNames in any shape
     * are using {@link NormalizedNodeStreamVersion#SODIUM_SR1}, which improves encoding.
     */
    SODIUM_SR1(7) {
        @Override
        public NormalizedNodeStreamVersion getStreamVersion() {
            return NormalizedNodeStreamVersion.SODIUM_SR1;
        }
    },

    /**
     * Revised payload version. Payloads remain the same as {@link #SODIUM_SR1}, but messages bearing QNames in any
     * shape are using {@link NormalizedNodeStreamVersion#MAGNESIUM}, which improves encoding.
     */
    MAGNESIUM(8) {
        @Override
        public NormalizedNodeStreamVersion getStreamVersion() {
            return NormalizedNodeStreamVersion.MAGNESIUM;
        }
    },

    /**
     * Version which is newer than any other version. This version exists purely for testing purposes.
     */
    @VisibleForTesting
    TEST_FUTURE_VERSION(65535) {
        @Override
        public NormalizedNodeStreamVersion getStreamVersion() {
            throw new UnsupportedOperationException();
        }
    };

    private final short value;

    PayloadVersion(final int intVersion) {
        checkArgument(intVersion >= 0 && intVersion <= 65535);
        value = (short) intVersion;
    }

    /**
     * Return the unsigned short integer identifying this version.
     *
     * @return Unsigned short integer identifying this version
     */
    public short shortValue() {
        return value;
    }

    /**
     * Return the NormalizedNode stream version corresponding to this particular ABI.
     *
     * @return Stream Version to use for this ABI version
     */
    public abstract @NonNull NormalizedNodeStreamVersion getStreamVersion();

    /**
     * Return the codebase-native persistence version. This version is the default version allocated to messages
     * at runtime. Conversion to previous versions may incur additional overhead (such as object allocation).
     *
     * @return Current {@link PayloadVersion}
     */
    public static @NonNull PayloadVersion current() {
        return MAGNESIUM;
    }

    /**
     * Return the {@link PayloadVersion} corresponding to an unsigned short integer. This method is provided for callers
     * which provide their own recovery strategy in case of version incompatibility.
     *
     * @param version Short integer as returned from {@link #shortValue()}
     * @return {@link PayloadVersion}
     * @throws FutureVersionException if the specified integer identifies a future version
     * @throws PastVersionException if the specified integer identifies a past version which is no longer supported
     */
    public static @NonNull PayloadVersion valueOf(final short version)
            throws FutureVersionException, PastVersionException {
        switch (Short.toUnsignedInt(version)) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                throw new PastVersionException(version, BORON);
            case 5:
                return BORON;
            case 6:
                return NEON_SR2;
            case 7:
                return SODIUM_SR1;
            case 8:
                return MAGNESIUM;
            default:
                throw new FutureVersionException(version, MAGNESIUM);
        }
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        out.writeShort(value);
    }

    /**
     * Read an {@link PayloadVersion} from a {@link DataInput}. This method is provided for callers which do not have
     * a recovery strategy for dealing with unsupported versions.
     *
     * @param in Input from which to read
     * @return An {@link PayloadVersion}
     * @throws IOException If read fails or an unsupported version is encountered
     */
    public static @NonNull PayloadVersion readFrom(final @NonNull DataInput in) throws IOException {
        final short s = in.readShort();
        try {
            return valueOf(s);
        } catch (FutureVersionException | PastVersionException e) {
            throw new IOException("Unsupported version", e);
        }
    }
}
