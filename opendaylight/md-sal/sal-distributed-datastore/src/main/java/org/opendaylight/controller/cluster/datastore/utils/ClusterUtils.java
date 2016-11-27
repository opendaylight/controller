/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Utils for encoding prefix shard name.
 */
public class ClusterUtils {

    public static ShardIdentifier getShardIdentifier(final MemberName memberName, final DOMDataTreeIdentifier prefix) {
        return ShardIdentifier
                .create(getCleanShardName(prefix.getRootIdentifier()), memberName, prefix.getDatastoreType().name());
    }

    /**
     * Returns an encoded shard name based on the provided path that should doesn't contain characters that cannot be
     * present in akka actor paths.
     *
     * @param path Path on which to base the shard name
     * @return encoded name that doesn't contain characters that cannot be in actor path.
     */
    public static String getCleanShardName(final YangInstanceIdentifier path) {
        final StringBuilder builder = new StringBuilder();
        // TODO need a better mapping that includes namespace, but we'll need to cleanup the string beforehand
        path.getPathArguments().forEach(p -> {
            builder.append(p.getNodeType().getLocalName());
            builder.append("!");
        });
        return builder.toString();
    }
}
