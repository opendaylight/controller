/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.gossip;

import java.io.Serializable;

/**
 * A tombstone marker indicating at which incarnation we switched to state files.
 */
final record Tombstone(int incarnation) implements Serializable {
    // Nothing else
}
