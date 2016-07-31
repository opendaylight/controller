/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.WritableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumeration of all ABI versions supported by this implementation of the client access API.
 *
 * @author Robert Varga
 */
@Beta
public enum ABIVersion implements WritableObject {
    // NOTE: enumeration values need to be sorted in asceding order of their version to keep Comparable working

    /**
     * Version which is older than any other version. This version exists purely for testing purposes.
     */
    @VisibleForTesting
    TEST_PAST_VERSION(0),

    /**
     * Initial ABI version, as shipped with Boron Simultaneous release.
     */
    // We seed the initial version to be the same as DataStoreVersions.BORON-VERSION for compatibility reasons.
    BORON(5),

    /**
     * Version which is newer than any other version. This version exists purely for testing purposes.
     */
    @VisibleForTesting
    TEST_FUTURE_VERSION(65535);

    private static final Logger LOG = LoggerFactory.getLogger(ABIVersion.class);

    private final short value;

    ABIVersion(final int intVersion) {
        Preconditions.checkArgument(intVersion >= 0 && intVersion <= 65535);
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
    public static @Nonnull ABIVersion current() {
        return BORON;
    }

    /**
     * Return the {@link ABIVersion} corresponding to an unsigned short integer. This method is provided for callers
     * which provide their own recovery strategy in case of version incompatibility.
     *
     * @param s Short integer as returned from {@link #shortValue()}
     * @return {@link ABIVersion}
     * @throws FutureVersionException if the specified integer identifies a future version
     * @throws PastVersionException if the specified integer identifies a past version which is no longer supported
     */
    public static @Nonnull ABIVersion valueOf(final short s) throws FutureVersionException, PastVersionException {
        switch (Short.toUnsignedInt(s)) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                throw new PastVersionException(s, BORON);
            case 5:
                return BORON;
            default:
                throw new FutureVersionException(s, BORON);
        }
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        out.writeShort(value);
    }

    /**
     * Read an {@link ABIVersion} from a {@link DataInput}. This method is provided for callers which do not have
     * a recovery strategy for dealing with unsupported versions.
     *
     * @param in Input from which to read
     * @return An {@link ABIVersion}
     * @throws IOException If read fails or an unsupported version is encountered
     */
    public static @Nonnull ABIVersion readFrom(final @Nonnull DataInput in) throws IOException {
        final short s = in.readShort();
        try {
            return valueOf(s);
        } catch (FutureVersionException | PastVersionException e) {
            throw new IOException("Unsupported version", e);
        }
    }

    public static ABIVersion inexactReadFrom(final @Nonnull DataInput in) throws IOException {
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
