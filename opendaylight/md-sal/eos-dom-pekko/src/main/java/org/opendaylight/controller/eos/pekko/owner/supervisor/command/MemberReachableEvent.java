/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.pekko.owner.supervisor.command;

import java.util.Set;
import org.apache.pekko.actor.Address;

public final class MemberReachableEvent extends InternalClusterEvent {
    public MemberReachableEvent(final Address address, final Set<String> roles) {
        super(address, roles);
    }
}
