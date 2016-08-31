/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.config;

import java.io.Serializable;
import java.util.Collection;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

public class PrefixShardConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    private final DOMDataTreeIdentifier prefix;
    private final String shardStrategyName;
    private final Collection<MemberName> shardMemberNames;

    public PrefixShardConfiguration(final DOMDataTreeIdentifier prefix, final String shardStrategyName, final Collection<MemberName> shardMemberNames) {
        this.prefix = prefix;
        this.shardStrategyName = shardStrategyName;
        this.shardMemberNames = shardMemberNames;
    }

    public DOMDataTreeIdentifier getPrefix() {
        return prefix;
    }

    public String getShardStrategyName() {
        return shardStrategyName;
    }

    public Collection<MemberName> getShardMemberNames() {
        return shardMemberNames;
    }
}
