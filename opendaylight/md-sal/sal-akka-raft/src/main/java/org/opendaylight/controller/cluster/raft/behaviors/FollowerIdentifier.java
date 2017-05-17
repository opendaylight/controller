/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import org.opendaylight.controller.cluster.messaging.StringIdentifier;
import org.opendaylight.yangtools.util.AbstractStringIdentifier;

/**
 * An Identifier for a follower.
 *
 * @author Thomas Pantelis
 */
class FollowerIdentifier extends AbstractStringIdentifier<StringIdentifier> {
    private static final long serialVersionUID = 1L;

    FollowerIdentifier(String followerId) {
        super(followerId);
    }
}
