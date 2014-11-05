
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager;

import java.io.Serializable;
import java.util.List;

import org.opendaylight.controller.sal.core.UpdateType;

public class ContainerFlowChangeEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<ContainerFlowConfig> configs;
    private UpdateType updateType;

    public ContainerFlowChangeEvent(List<ContainerFlowConfig> configs, UpdateType updateType) {
        this.configs = configs;
        this.updateType = updateType;
    }

    public List<ContainerFlowConfig> getConfigList() {
        return configs;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    @Override
    public String toString() {
        return "ContainerFlowChangeEvent [configs: " + configs + " updateType: " + updateType + "]";
    }
}
