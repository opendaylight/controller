/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.util.ArrayDeque;
import java.util.concurrent.BlockingQueue;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.PekkoRaftStorage.JournalAction;

@NonNullByDefault
final class JournalWriter implements Runnable {
    private final BlockingQueue<JournalAction> incoming;

    JournalWriter(final BlockingQueue<JournalAction> incoming) {
        this.incoming = requireNonNull(incoming);
    }

    @Override
    public void run() {
        final var batch = new ArrayDeque<JournalAction>();

        while (true) {
            // Attempt to drain all elements first
            incoming.drainTo(batch);
            if (batch.isEmpty()) {
                // Nothing to do: wait for some work to show up
                final JournalAction first;
                try {
                    first = incoming.take();
                } catch (InterruptedException e) {
                    // Should never happen, really
                    throw new IllegalStateException("interrupted while waiting, waiting for next command", e);
                }

                // We have an entry, let's process it
                batch.add(first);
            }






        }




        // TODO Auto-generated method stub

    }



}