/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * An {@link InputStreamProvider} with a known size.
 */
@NonNullByDefault
public interface SizedDataSource extends InputStreamProvider {
    /**
     * Returns the size of this source.
     *
     * @return the size of this source
     */
    int size();
}
