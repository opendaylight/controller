/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * Enumeration of all ABI versions supported by this implementation of the client access API.
 *
 * @author Robert Varga
 */
public enum ABIVersion implements Comparable<ABIVersion> {
    /**
     * Version which is older than any other version. This version exists purely for testing purposes.
     */
    @VisibleForTesting
    TEST_PAST_VERSION(0),

    /**
     * Initial ABI version, as shipped with Boron Simultaneous release.
     */
    BORON(1),

    /**
     * Version which is newer than any other version. This version exists purely for testing purposes.
     */
    @VisibleForTesting
    TEST_FUTURE_VERSION(65535);

    private final short value;

    ABIVersion(final int intVersion) {
        Preconditions.checkArgument(intVersion >= 0 && intVersion <= 65535);
        value = (short) intVersion;
    }

    /**
     * Return the unsigned short integer identifying this version.
     *
     * @return
     */
    public short shortValue() {
        return value;
    }

    /**
     * Return the {@link ABIVersion} corresponding to an unsigned short integer.
     *
     * @param s Short integer as returned from {@link #shortValue()}
     * @return {@link ABIVersion}
     * @throws FutureVersionException if the specified integer identifies a future version
     * @throws PastVersionException if the specified integer identifies a past version which is no longer supported
     */
    public static @Nonnull ABIVersion valueOf(final short s) throws FutureVersionException, PastVersionException {
        switch (s) {
            case 0:
                throw new PastVersionException(s, BORON);
            case 1:
                return BORON;
            default:
                throw new FutureVersionException(s, BORON);
        }
    }
}
