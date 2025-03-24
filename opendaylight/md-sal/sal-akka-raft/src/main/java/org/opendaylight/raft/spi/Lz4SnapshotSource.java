/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

/**
 * A LZ4-compressed {@link SnapshotSource}.
 */
public non-sealed interface Lz4SnapshotSource extends SnapshotSource {
    // Nothing else
}
