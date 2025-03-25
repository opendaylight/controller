/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.api.TermInfo;

@NonNullByDefault
public final class TestTermInfoStore extends AbstractTermInfoStore  {
    public TestTermInfoStore() {
        super();
    }

    public TestTermInfoStore(final long term, final @Nullable String votedFor) {
        super(term, votedFor);
    }

    @Override
    public void storeAndSetTerm(final TermInfo newElectionInfo) {
        setTerm(newElectionInfo);
    }
}
