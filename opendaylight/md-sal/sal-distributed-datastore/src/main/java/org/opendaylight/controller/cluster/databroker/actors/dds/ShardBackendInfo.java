/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import java.util.Optional;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.client.BackendInfo;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

/**
 * Combined backend tracking. Aside from usual {@link BackendInfo}, this object also tracks the cookie assigned
 * to the shard. This assignment remains constant for as long as the client is not restarted.
 *
 * @author Robert Varga
 */
@ThreadSafe
final class ShardBackendInfo extends BackendInfo {
    private final Optional<DataTree> dataTree;
    private final UnsignedLong cookie;
    private final String shardName;

    ShardBackendInfo(final ActorRef actor, final long sessionId, final ABIVersion version, final String shardName,
        final UnsignedLong cookie, final Optional<DataTree> dataTree, final int maxMessages) {
        super(actor, sessionId, version, maxMessages);
        this.shardName = Preconditions.checkNotNull(shardName);
        this.cookie = Preconditions.checkNotNull(cookie);
        this.dataTree = Preconditions.checkNotNull(dataTree);
    }

    UnsignedLong getCookie() {
        return cookie;
    }

    Optional<DataTree> getDataTree() {
        return dataTree;
    }

    String getShardName() {
        return shardName;
    }

    LocalHistoryIdentifier brandHistory(final LocalHistoryIdentifier id) {
        Preconditions.checkArgument(id.getCookie() == 0, "History %s is already branded", id);
        return new LocalHistoryIdentifier(id.getClientId(), id.getHistoryId(), cookie.longValue());
    }

    @Override
    protected ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("cookie", cookie).add("shard", shardName);
    }
}
