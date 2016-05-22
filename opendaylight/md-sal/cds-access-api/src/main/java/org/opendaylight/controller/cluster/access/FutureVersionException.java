/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access;

import com.google.common.annotations.Beta;

/**
 * Exception thrown from {@link ABIVersion#valueOf(short)} when the specified version is too new to be supported
 * by the codebase.
 *
 * @author Robert Varga
 */
@Beta
public final class FutureVersionException extends AbstractVersionException {
    private static final long serialVersionUID = 1L;

    FutureVersionException(final short version, ABIVersion closest) {
        super("Version " + Short.toUnsignedInt(version) + " is too new", version, closest);
    }
}
