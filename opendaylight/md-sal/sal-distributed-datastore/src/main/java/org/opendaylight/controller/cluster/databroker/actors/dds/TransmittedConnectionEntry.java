/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

final class TransmittedConnectionEntry extends ConnectionEntry {
    private final long sessionId;
    private final long txSequence;
    private final long txTicks;

    TransmittedConnectionEntry(final ConnectionEntry entry, final long sessionId, final long txSequence,
        final long now) {
        super(entry);
        this.sessionId = sessionId;
        this.txSequence = txSequence;
        this.txTicks = now;
    }

}
