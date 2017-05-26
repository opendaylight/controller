/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;

/**
 * Abstract base exception used for reporting version mismatches from {@link PayloadVersion}.
 *
 * @author Robert Varga
 */
@Beta
public abstract class AbstractVersionException extends Exception {
    private static final long serialVersionUID = 1L;
    private final PayloadVersion closestVersion;
    private final int version;

    AbstractVersionException(final String message, final short version, final PayloadVersion closestVersion) {
        super(message);
        this.closestVersion = Preconditions.checkNotNull(closestVersion);
        this.version = Short.toUnsignedInt(version);
    }

    /**
     * Return the numeric version which has caused this exception.
     *
     * @return Numeric version
     */
    public final int getVersion() {
        return version;
    }

    /**
     * Return the closest version supported by this codebase.
     *
     * @return Closest supported {@link PayloadVersion}
     */
    public final @Nonnull PayloadVersion getClosestVersion() {
        return closestVersion;
    }

}
