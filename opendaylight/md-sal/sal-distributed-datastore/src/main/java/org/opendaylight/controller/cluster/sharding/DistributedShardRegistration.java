/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import com.google.common.annotations.Beta;
import java.util.concurrent.CompletionStage;

/**
 * Registration of the CDS shard that allows you to remove the shard from the system by closing the registration.
 * This removal is done asynchronously.
 */
@Beta
@Deprecated(forRemoval = true)
public interface DistributedShardRegistration {

    /**
     *  Removes the shard from the system, this removal is done asynchronously, the future completes once the
     *  backend shard is no longer present.
     */
    CompletionStage<Void> close();
}
