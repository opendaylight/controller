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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for encoding prefix shard name.
 */
public final class ClusterUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterUtils.class);

    // id for the shard used to store prefix configuration
    public static final String PREFIX_CONFIG_SHARD_ID = "prefix-configuration-shard";

    public static final QName PREFIX_SHARDS_QNAME =
            QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:clustering:prefix-shard-configuration",
                    "2017-01-10", "prefix-shards").intern();
    public static final QName SHARD_LIST_QNAME =
            QName.create(PREFIX_SHARDS_QNAME, "shard").intern();
    public static final QName SHARD_PREFIX_QNAME =
            QName.create(PREFIX_SHARDS_QNAME, "prefix").intern();
    public static final QName SHARD_PERSISTENCE_QNAME =
            QName.create(PREFIX_SHARDS_QNAME, "persistence").intern();
    public static final QName SHARD_PERSISTENCE_DATASTORE_QNAME =
            QName.create(PREFIX_SHARDS_QNAME, "datastore").intern();
    public static final QName SHARD_PERSISTENCE_PERSISTENT_QNAME =
            QName.create(PREFIX_SHARDS_QNAME, "persistent").intern();
    public static final QName SHARD_REPLICAS_QNAME =
            QName.create(PREFIX_SHARDS_QNAME, "replicas").intern();
    public static final QName SHARD_REPLICA_QNAME =
            QName.create(PREFIX_SHARDS_QNAME, "replica").intern();

    public static final YangInstanceIdentifier PREFIX_SHARDS_PATH =
            YangInstanceIdentifier.of(PREFIX_SHARDS_QNAME).toOptimized();
    public static final YangInstanceIdentifier SHARD_LIST_PATH =
            PREFIX_SHARDS_PATH.node(SHARD_LIST_QNAME).toOptimized();

    private ClusterUtils() {
    }

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
                LOG.warn("Unknown data store type {}", type);
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
                ((NodeIdentifierWithPredicates) p).entrySet().forEach(entry -> {
                    builder.append(entry.getKey().getLocalName()).append(entry.getValue()).append('-');
                });
                builder.append('_');
            }
            builder.append('!');
        });
        return builder.toString();
    }
}
