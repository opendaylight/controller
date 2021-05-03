/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka.owner.supervisor.command;

import java.io.Serializable;

public final class DataCenterDeactivated extends OwnerSupervisorReply implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final DataCenterDeactivated INSTANCE = new DataCenterDeactivated();

    private DataCenterDeactivated() {
        // NOOP
    }
}
