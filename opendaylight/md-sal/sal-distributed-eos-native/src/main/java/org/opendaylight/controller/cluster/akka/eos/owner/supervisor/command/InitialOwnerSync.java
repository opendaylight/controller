/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command;

import akka.cluster.ddata.LWWRegister;
import akka.cluster.ddata.typed.javadsl.Replicator;

public class InitialOwnerSync implements OwnerSupervisorCommand {

    private final Replicator.GetResponse<LWWRegister<String>> response;

    public InitialOwnerSync(final Replicator.GetResponse<LWWRegister<String>> response) {
        this.response = response;
    }

    public Replicator.GetResponse<LWWRegister<String>> getResponse() {
        return response;
    }
}
