/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Actor with its behaviour metered. Metering is enabled by configuration.
 */
public abstract class AbstractUntypedPersistentActorWithMetering extends AbstractUntypedPersistentActor {
    @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", justification = "Akka class design")
    public AbstractUntypedPersistentActorWithMetering() {
        if (isMetricsCaptureEnabled()) {
            getContext().become(new MeteringBehavior(this));
        }
    }

    private boolean isMetricsCaptureEnabled() {
        return new CommonConfig(getContext().system().settings().config()).isMetricCaptureEnabled();
    }
}
