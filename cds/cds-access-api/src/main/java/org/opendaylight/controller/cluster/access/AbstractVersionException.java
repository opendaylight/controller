/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Abstract base exception used for reporting version mismatches from {@link ABIVersion}.
 */
public abstract class AbstractVersionException extends Exception {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @NonNull ABIVersion closestVersion;
    private final int version;

    AbstractVersionException(final String message, final short version, final ABIVersion closestVersion) {
        super(message);
        this.closestVersion = requireNonNull(closestVersion);
        this.version = Short.toUnsignedInt(version);
    }

    /**
     * Return the numeric version which has caused this exception.
     *
     * @return Numeric version
     */
    public final int version() {
        return version;
    }

    /**
     * Return the closest version supported by this codebase.
     *
     * @return Closest supported {@link ABIVersion}
     */
    public final @NonNull ABIVersion closestVersion() {
        return closestVersion;
    }
}
