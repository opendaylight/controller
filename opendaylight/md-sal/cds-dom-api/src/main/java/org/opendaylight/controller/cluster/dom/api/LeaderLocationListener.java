/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.dom.api;

import com.google.common.annotations.Beta;
import java.util.EventListener;
import javax.annotation.Nonnull;

/**
 * Listener for shard leader location changes.
 *
 * @author Robert Varga
 */
@Beta
public interface LeaderLocationListener extends EventListener {
    /**
     * Invoked when shard leader location changes.
     *
     * @param location Current leader location as known by the local node.
     */
    void onLeaderLocationChanged(@Nonnull LeaderLocation location);
}
