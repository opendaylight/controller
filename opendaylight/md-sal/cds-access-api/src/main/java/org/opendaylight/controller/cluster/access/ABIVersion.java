/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableObject;
import org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumeration of all ABI versions supported by this implementation of the client access API.
 */
public enum ABIVersion implements WritableObject {
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

    // BORON was 5
    // NEON_SR2 was 6
    // SODIUM_SR1 was 7
    // MAGNESIUM was 8
    // CHLORINE_SR2 was 9

    /**
     * Oldest ABI version we support. The messages remain the same as {@code CHLORINE_SR2}, the serialization proxies in
     * use are flat objects without any superclasses. Data encoding does not include augmentations as separate objects.
     */
    POTASSIUM(10) {
        @Override
        public NormalizedNodeStreamVersion getStreamVersion() {
            return NormalizedNodeStreamVersion.POTASSIUM;
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

    private static final Logger LOG = LoggerFactory.getLogger(ABIVersion.class);

    private final short value;

    ABIVersion(final int intVersion) {
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
     * Return the codebase-native ABI version. This version is the default version allocated to messages at runtime.
     * Conversion to previous versions may incur additional overhead (such as object allocation).
     *
     * @return Current {@link ABIVersion}
     */
    public static @NonNull ABIVersion current() {
        return POTASSIUM;
    }

    /**
     * Return the {@link ABIVersion} corresponding to an unsigned short integer. This method is provided for callers
     * which provide their own recovery strategy in case of version incompatibility.
     *
     * @param value Short integer as returned from {@link #shortValue()}
     * @return {@link ABIVersion}
     * @throws FutureVersionException if the specified integer identifies a future version
     * @throws PastVersionException if the specified integer identifies a past version which is no longer supported
     */
    public static @NonNull ABIVersion valueOf(final short value) throws FutureVersionException, PastVersionException {
        return switch (Short.toUnsignedInt(value)) {
            case 0, 1, 2, 3, 4, 6, 7, 8, 9 -> throw new PastVersionException(value, POTASSIUM);
            case 10 -> POTASSIUM;
            default -> throw new FutureVersionException(value, POTASSIUM);
        };
    }

    /**
     * Return {@code true} if this version is earier than some {@code other} version.
     *
     * @param other Other {@link ABIVersion}
     * @return {@code true} if {@code other is later}
     * @throws NullPointerException if {@code other} is null
     */
    public boolean lt(final @NonNull ABIVersion other) {
        return compareTo(other) < 0;
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        out.writeShort(value);
    }

    /**
     * Return the NormalizedNode stream version corresponding to this particular ABI.
     *
     * @return Stream Version to use for this ABI version
     */
    public abstract @NonNull NormalizedNodeStreamVersion getStreamVersion();

    /**
     * Read an {@link ABIVersion} from a {@link DataInput}. This method is provided for callers which do not have
     * a recovery strategy for dealing with unsupported versions.
     *
     * @param in Input from which to read
     * @return An {@link ABIVersion}
     * @throws IOException If read fails or an unsupported version is encountered
     */
    public static @NonNull ABIVersion readFrom(final @NonNull DataInput in) throws IOException {
        final short s = in.readShort();
        try {
            return valueOf(s);
        } catch (FutureVersionException | PastVersionException e) {
            throw new IOException("Unsupported version", e);
        }
    }

    public static @NonNull ABIVersion inexactReadFrom(final @NonNull DataInput in) throws IOException {
        final short onWire = in.readShort();
        try {
            return ABIVersion.valueOf(onWire);
        } catch (FutureVersionException e) {
            LOG.debug("Received future version", e);
            return ABIVersion.TEST_FUTURE_VERSION;
        } catch (PastVersionException e) {
            LOG.debug("Received past version", e);
            return ABIVersion.TEST_PAST_VERSION;
        }
    }
}
