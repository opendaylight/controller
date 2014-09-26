/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

/**
 * Actor with its behaviour metered. Metering is enabled by configuration.
 */
public abstract class AbstractUntypedActorWithMetering extends AbstractUntypedActor {

    //this is used in the metric name. Some transient actors do not have defined names
    private String actorNameOverride;

    public AbstractUntypedActorWithMetering() {
        if (isMetricsCaptureEnabled())
            getContext().become(new MeteringBehavior(this));
    }

    public AbstractUntypedActorWithMetering(String actorNameOverride){
        this.actorNameOverride = actorNameOverride;
        if (isMetricsCaptureEnabled())
            getContext().become(new MeteringBehavior(this));
    }

    private boolean isMetricsCaptureEnabled(){
        CommonConfig config = new CommonConfig(getContext().system().settings().config());
        return config.isMetricCaptureEnabled();
    }

    public String getActorNameOverride() {
        return actorNameOverride;
    }
}
