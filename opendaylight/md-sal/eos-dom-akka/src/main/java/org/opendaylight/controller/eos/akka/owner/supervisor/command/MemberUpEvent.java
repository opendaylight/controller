/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import akka.actor.Address;
import java.util.Set;

public final class MemberUpEvent extends InternalClusterEvent {
    public MemberUpEvent(final Address address, final Set<String> roles) {
        super(address, roles);
    }
}
