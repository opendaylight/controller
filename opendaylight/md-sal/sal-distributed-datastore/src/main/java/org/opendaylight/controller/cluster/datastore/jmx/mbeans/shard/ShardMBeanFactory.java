/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.Shard;

/**
 * Factory for creating ShardStats mbeans.
 *
 * @author Basheeruddin syedbahm@cisco.com
 */
public class ShardMBeanFactory {

    public static ShardStats getShardStatsMBean(final String shardName, final String mxBeanType,
            @Nonnull final Shard shard) {
        String finalMXBeanType = mxBeanType != null ? mxBeanType : "DistDataStore";
        ShardStats shardStatsMBeanImpl = new ShardStats(shardName, finalMXBeanType, shard);
        shardStatsMBeanImpl.registerMBean();
        return shardStatsMBeanImpl;
    }
}
