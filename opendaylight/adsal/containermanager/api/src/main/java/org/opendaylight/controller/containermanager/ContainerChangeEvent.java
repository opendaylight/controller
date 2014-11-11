
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager;

import java.io.Serializable;

import org.opendaylight.controller.sal.core.UpdateType;

public class ContainerChangeEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private ContainerConfig config;
    private UpdateType update;

    public ContainerChangeEvent(ContainerConfig config, UpdateType update) {
        this.config = config;
        this.update = update;
    }

    public UpdateType getUpdateType() {
        return update;
    }

    public ContainerConfig getConfig() {
        return config;
    }
}
