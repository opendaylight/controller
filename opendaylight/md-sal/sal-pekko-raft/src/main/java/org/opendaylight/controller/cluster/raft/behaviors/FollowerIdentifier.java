/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import org.opendaylight.yangtools.util.AbstractStringIdentifier;

/**
 * An Identifier for a follower.
 *
 * @author Thomas Pantelis
 */
final class FollowerIdentifier extends AbstractStringIdentifier<FollowerIdentifier> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    FollowerIdentifier(final String followerId) {
        super(followerId);
    }

    @java.io.Serial
    private Object writeReplace() {
        return new FI(getValue());
    }
}
