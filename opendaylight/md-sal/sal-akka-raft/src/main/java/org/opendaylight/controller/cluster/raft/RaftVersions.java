/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

/**
 * @author Thomas Pantelis
 */
public interface RaftVersions {
    short HELIUM_VERSION = 0;
    short LITHIUM_VERSION = 1;
    short CURRENT_VERSION = LITHIUM_VERSION;
}
