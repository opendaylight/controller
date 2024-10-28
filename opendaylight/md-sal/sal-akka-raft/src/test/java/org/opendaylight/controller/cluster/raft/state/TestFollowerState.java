/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.ScatteringByteChannel;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public record TestFollowerState(FollowerStuff stuff) implements FollowerState {
    public TestFollowerState {
        requireNonNull(stuff);
    }

    @Override
    public final short payloadVersion() {
        return stuff.payloadVersion;
    }

    @Override
    public InactiveState toInactive() {
        return TestInactive.INSTANCE;
    }

    @Override
    public CandidateState toCandidate() {
        return new TestCandidateState(new InactiveStuff(stuff));
    }

    @Override
    public void applyEntry(final ScatteringByteChannel in) throws IOException, FollowerStateException {
        stuff.applyEntry(in);
    }

    @Override
    public StateSnapshot takeSnapshot() {
        return stuff.takeSnapshot();
    }
}
