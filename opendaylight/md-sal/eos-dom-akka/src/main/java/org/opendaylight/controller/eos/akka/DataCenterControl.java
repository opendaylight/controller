/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * Service used to bring up/down the NativeEos service in individual datacenters.
 * Active datacenter in native eos terms means that the candidates from this datacenter can become owners of entities.
 * Additionally the singleton component makings ownership decisions, runs only in an active datacenter.
 *
 * <p>
 * Caller must make sure that only one datacenter is active at a time, otherwise the singleton actors
 * in each datacenter will interfere with each other. The methods provided byt this service can be called
 * on any node from the datacenter to be activated. Datacenters only need to brought up when using non-default
 * datacenter or multiple datacenters.
 */
@Beta
public interface DataCenterControl {
    /**
     * Activates the native eos service in the datacenter that this method is called.
     */
    @NonNull ListenableFuture<Empty> activateDataCenter();
    /**
     * Deactivates the native eos service in the datacenter that this method is called.
     *
     * @return Completion future
     */
    @NonNull ListenableFuture<Empty> deactivateDataCenter();
}
