/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import java.util.Optional;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A component capable of instantiating a {@link RaftPolicy} from its symbolic name.
 */
@NonNullByDefault
public interface RaftPolicyResolver {
   /**
     * {@return the {@link RaftPolicy} corresponding to a symbolic name, or emptyif not present}
     * @param symbolicName the symbolic name
     */
    Optional<RaftPolicy> findRaftPolicy(String symbolicName);
}
