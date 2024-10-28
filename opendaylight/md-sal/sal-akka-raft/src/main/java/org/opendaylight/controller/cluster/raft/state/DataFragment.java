/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import java.io.IOException;
import java.nio.channels.GatheringByteChannel;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.Mutable;

/**
 * A handle to a piece of data that can be subject to {@link #writeTo(GatheringByteChannel)} exactly once. This
 * interface is {@link Mutable}, but the underlying data has to be immutable.
 */
@NonNullByDefault
public interface DataFragment extends Mutable {

    // Note: this method will be invoked exactly once
    void writeTo(GatheringByteChannel out) throws IOException;
}
