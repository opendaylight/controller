/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

/**
 */
public abstract class AbstractUntypedActorWithMetering extends AbstractUntypedActor {

    public AbstractUntypedActorWithMetering() {
        if (isMetricsCaptureEnabled())
            getContext().become(new MeteringBehavior(this));
    }

    private boolean isMetricsCaptureEnabled(){
        CommonConfig config = new CommonConfig(getContext().system().settings().config());
        return config.isMetricCaptureEnabled();
    }
}
