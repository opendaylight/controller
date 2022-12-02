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
    @Deprecated(since = "7.0.0", forRemoval = true)
    public static final short HELIUM_VERSION = 0;
    @Deprecated(since = "7.0.0", forRemoval = true)
    public static final short LITHIUM_VERSION = 1;
    @Deprecated(since = "7.0.0", forRemoval = true)
    public static final short BORON_VERSION = 3;
    public static final short FLUORINE_VERSION = 4;
    public static final short ARGON_VERSION = 5;
    public static final short CURRENT_VERSION = ARGON_VERSION;

    private RaftVersions() {

    }
}
