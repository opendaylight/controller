/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public final class FailingTermInfoStore extends AbstractTermInfoStore {
    public FailingTermInfoStore(final long term, final String votedFor) {
        super(term, votedFor);
    }

    @Override
    public void storeAndSetTerm(final TermInfo newTerm) {
        throw new UnsupportedOperationException();
    }
}
