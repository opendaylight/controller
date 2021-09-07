/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Codec providing translation between CLI representation and {@link InstanceIdentifier}. This is mostly useful for
 * injecting invocation contexts for {@code routed RPC}s and actions.
 */
public interface InstanceIdentifierSupport {
    /**
     * Parse a CLI argument into its {@link InstanceIdentifier} representation.
     *
     * @param argument Argument to parse
     * @return Parse InstanceIdentifier
     * @throws NullPointerException if {@code argument} is null
     */
    @NonNull InstanceIdentifier<?> parseArgument(String argument);
}
