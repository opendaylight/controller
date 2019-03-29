/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * A message sent to the ShardManager to dynamically remove a local prefix shard
 *  replica available in this node.
 */
public class RemovePrefixShardReplica {

    private final YangInstanceIdentifier prefix;
    private final MemberName memberName;

    /**
     * Constructor.
     *
     * @param prefix prefix of the local shard that is to be dynamically removed.
     */
    public RemovePrefixShardReplica(final @NonNull YangInstanceIdentifier prefix,
                                    final @NonNull MemberName memberName) {
        this.prefix = requireNonNull(prefix, "prefix should not be null");
        this.memberName = requireNonNull(memberName, "memberName should not be null");
    }

    public YangInstanceIdentifier getShardPrefix() {
        return prefix;
    }

    public MemberName getMemberName() {
        return memberName;
    }

    @Override
    public String toString() {
        return "RemovePrefixShardReplica [prefix=" + prefix + ", memberName=" + memberName + "]";
    }
}
