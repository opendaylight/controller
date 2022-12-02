/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

/**
 * Enumerates the raft versions.
 *
 * @author Thomas Pantelis
 */
public final class RaftVersions {
    // HELIUM_VERSION = 0
    // LITHIUM_VERSION = 1
    // BORON_VERSION = 3
    public static final short FLUORINE_VERSION = 4;
    public static final short ARGON_VERSION = 5;
    public static final short CURRENT_VERSION = ARGON_VERSION;

    private RaftVersions() {
        // Hidden on purpose
    }
}
