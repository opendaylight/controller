/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import akka.cluster.ddata.Key;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORMapKey;
import java.util.Map;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

/**
 * Utils for encoding prefix shard name.
 */
public class ClusterUtils {

    // key for replicated configuration key
    public static final Key<ORMap<PrefixShardConfiguration>> CONFIGURATION_KEY =
            ORMapKey.create("prefix-shard-configuration-config");

    public static final Key<ORMap<PrefixShardConfiguration>> OPERATIONAL_KEY =
            ORMapKey.create("prefix-shard-configuration-oper");

    public static ShardIdentifier getShardIdentifier(final MemberName memberName, final DOMDataTreeIdentifier prefix) {
        final String type;
        switch (prefix.getDatastoreType()) {
            case OPERATIONAL:
                type = "operational";
                break;
            case CONFIGURATION:
                type = "config";
                break;
            default:
                type = prefix.getDatastoreType().name();
        }

        return ShardIdentifier.create(getCleanShardName(prefix.getRootIdentifier()), memberName, type);
    }

    /**
     * Returns an encoded shard name based on the provided path that should doesn't contain characters that cannot be
     * present in akka actor paths.
     *
     * @param path Path on which to base the shard name
     * @return encoded name that doesn't contain characters that cannot be in actor path.
     */
    public static String getCleanShardName(final YangInstanceIdentifier path) {
        if (path.isEmpty()) {
            return "default";
        }

        final StringBuilder builder = new StringBuilder();
        // TODO need a better mapping that includes namespace, but we'll need to cleanup the string beforehand
        // we have to fight both javax and akka url path restrictions..
        path.getPathArguments().forEach(p -> {
            builder.append(p.getNodeType().getLocalName());
            if (p instanceof NodeIdentifierWithPredicates) {
                builder.append("-key_");
                final Map<QName, Object> key = ((NodeIdentifierWithPredicates) p).getKeyValues();
                key.entrySet().forEach(e -> {
                    builder.append(e.getKey().getLocalName());
                    builder.append(e.getValue());
                    builder.append("-");
                });
                builder.append("_");
            }
            builder.append("!");
        });
        return builder.toString();
    }

    public static Key<ORMap<PrefixShardConfiguration>> getReplicatorKey(LogicalDatastoreType type) {
        if (LogicalDatastoreType.CONFIGURATION.equals(type)) {
            return CONFIGURATION_KEY;
        } else {
            return OPERATIONAL_KEY;
        }
    }

    public static org.opendaylight.mdsal.common.api.LogicalDatastoreType toMDSalApi(
            final LogicalDatastoreType logicalDatastoreType) {
        return org.opendaylight.mdsal.common.api.LogicalDatastoreType.valueOf(logicalDatastoreType.name());
    }
}
