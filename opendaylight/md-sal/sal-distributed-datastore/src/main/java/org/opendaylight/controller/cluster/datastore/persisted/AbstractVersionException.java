/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Abstract base exception used for reporting version mismatches from {@link PayloadVersion}.
 */
@NonNullByDefault
public abstract sealed class AbstractVersionException extends Exception
        permits FutureVersionException, PastVersionException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final PayloadVersion closestVersion;
    private final int version;

    AbstractVersionException(final String message, final short version, final PayloadVersion closestVersion) {
        super(message);
        this.closestVersion = requireNonNull(closestVersion);
        this.version = Short.toUnsignedInt(version);
    }

    /**
     * {@return the numeric version which has caused this exception}
     */
    public final int getVersion() {
        return version;
    }

    /**
     * {@return the closest version supported by this codebase}
     */
    public final PayloadVersion getClosestVersion() {
        return closestVersion;
    }
}
