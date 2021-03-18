/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import static java.util.Objects.requireNonNull;

import akka.actor.Address;
import com.google.common.base.MoreObjects;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;

public abstract class InternalClusterEvent extends OwnerSupervisorCommand {
    private final @NonNull Set<String> roles;
    private final @NonNull Address address;

    InternalClusterEvent(final Address address, final Set<String> roles) {
        this.address = requireNonNull(address);
        this.roles = Set.copyOf(roles);
    }

    public final @NonNull Address getAddress() {
        return address;
    }

    public final @NonNull Set<String> getRoles() {
        return roles;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("address", address).add("roles", roles).toString();
    }
}
