/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import static akka.actor.ActorRef.noSender;
import static org.opendaylight.controller.cluster.datastore.utils.ClusterUtils.SHARD_PREFIX_QNAME;
import static org.opendaylight.controller.cluster.datastore.utils.ClusterUtils.SHARD_REPLICAS_QNAME;
import static org.opendaylight.controller.cluster.datastore.utils.ClusterUtils.SHARD_REPLICA_QNAME;

import akka.actor.ActorRef;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.controller.cluster.datastore.shardstrategy.PrefixShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardCreated;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardRemoved;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShardConfigUpdateHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ShardConfigUpdateHandler.class);
    private final ActorRef handlingActor;
    private final MemberName memberName;

    private final EnumMap<LogicalDatastoreType,
            ListenerRegistration<org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener>> registrations =
            new EnumMap<>(LogicalDatastoreType.class);

    public ShardConfigUpdateHandler(final ActorRef handlingActor, final MemberName memberName) {
        this.handlingActor = handlingActor;
        this.memberName = memberName;
    }

    public void initListener(final AbstractDataStore dataStore, final LogicalDatastoreType type) {
        registrations.put(type, dataStore.registerShardConfigListener(
                ClusterUtils.SHARD_LIST_PATH, new ShardConfigHandler(memberName, type, handlingActor)));
    }

    public void close() {
        registrations.values().forEach(ListenerRegistration::close);
        registrations.clear();
    }

    public static final class ShardConfigHandler implements ClusteredDOMDataTreeChangeListener {

        private final MemberName memberName;
        private final LogicalDatastoreType type;
        private final ActorRef handlingActor;

        public ShardConfigHandler(final MemberName memberName,
                           final LogicalDatastoreType type,
                           final ActorRef handlingActor) {
            this.memberName = memberName;
            this.type = type;
            this.handlingActor = handlingActor;
        }

        @Override
        public void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes) {
            changes.forEach(this::resolveChange);
        }

        private void resolveChange(final DataTreeCandidate candidate) {
            switch (candidate.getRootNode().getModificationType()) {
                case UNMODIFIED:
                    break;
                case SUBTREE_MODIFIED:
                case APPEARED:
                case WRITE:
                    resolveWrite(candidate.getRootNode());
                    break;
                case DELETE:
                case DISAPPEARED:
                    resolveDelete(candidate.getRootNode());
                    break;
                default:
                    break;
            }
        }

        private void resolveWrite(final DataTreeCandidateNode rootNode) {

            LOG.warn("{}: New config received {}", memberName, rootNode);
            LOG.warn("{}: Data after: {}", memberName, rootNode.getDataAfter());

            // were in the shards list, iter children and resolve
            for (final DataTreeCandidateNode childNode : rootNode.getChildNodes()) {
                switch (childNode.getModificationType()) {
                    case UNMODIFIED:
                        break;
                    case SUBTREE_MODIFIED:
                    case APPEARED:
                    case WRITE:
                        resolveWrittenShard(childNode);
                        break;
                    case DELETE:
                    case DISAPPEARED:
                        resolveDeletedShard(childNode);
                        break;
                    default:
                        break;
                }
            }

//            final MapEntryNode shardNode = (MapEntryNode) rootNode.getDataAfter().get();
//
//            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> child =
//                    shardNode.getChild(new NodeIdentifier(ShardConfigWriter.SHARD_PREFIX_QNAME)).get();

        }

        private void resolveWrittenShard(final DataTreeCandidateNode childNode) {
            final MapEntryNode entryNode = (MapEntryNode) childNode.getDataAfter().get();
            final LeafNode<String> prefix =
                    (LeafNode<String>) entryNode.getChild(new NodeIdentifier(SHARD_PREFIX_QNAME)).get();

            final YangInstanceIdentifier identifier = deserializePath(prefix.getValue());

            LOG.debug("{}: Deserialized {} from datastore", memberName, identifier);

            final ContainerNode replicas =
                    (ContainerNode) entryNode.getChild(new NodeIdentifier(SHARD_REPLICAS_QNAME)).get();

            final LeafSetNode<String> replicaList =
                    (LeafSetNode<String>) replicas.getChild(new NodeIdentifier(SHARD_REPLICA_QNAME)).get();

            final List<MemberName> retReplicas = replicaList.getValue().stream()
                    .map(child -> MemberName.forName(child.getValue()))
                    .collect(Collectors.toList());

            LOG.debug("{}: Replicas read from ds [}", memberName, retReplicas.toString());

            final PrefixShardConfiguration newConfig =
                    new PrefixShardConfiguration(new DOMDataTreeIdentifier(type, identifier),
                            PrefixShardStrategy.NAME, retReplicas);

            LOG.debug("{}: Resulting config {}", memberName, newConfig);

            handlingActor.tell(new PrefixShardCreated(newConfig), noSender());
        }

        private void resolveDeletedShard(final DataTreeCandidateNode childNode) {

            final MapEntryNode entryNode = (MapEntryNode) childNode.getDataBefore().get();

            final LeafNode<String> prefix =
                    (LeafNode<String>) entryNode.getChild(new NodeIdentifier(SHARD_PREFIX_QNAME)).get();

            final YangInstanceIdentifier deleted = deserializePath(prefix.getValue());
            LOG.debug("{}: Removing shard at {}.", memberName, deleted);

            final DOMDataTreeIdentifier domDataTreeIdentifier = new DOMDataTreeIdentifier(type, deleted);
            final PrefixShardRemoved message = new PrefixShardRemoved(domDataTreeIdentifier);

            handlingActor.tell(message, noSender());
        }

        private void resolveDelete(final DataTreeCandidateNode rootNode) {

        }
    }

    private static YangInstanceIdentifier deserializePath(final String serialized) {
        final ByteArrayInputStream inputStream =
                new ByteArrayInputStream(serialized.getBytes(StandardCharsets.ISO_8859_1));
        final DataInputStream dataInputStream = new DataInputStream(inputStream);
        final YangInstanceIdentifier deserialized = SerializationUtils.deserializePath(dataInputStream);

        try {
            dataInputStream.close();
        } catch (IOException e) {
            LOG.error("Unable to close stream used for deserialization of {}", deserialized, e);
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            LOG.error("Unable to close stream used for deserialization of {}", deserialized, e);
        }

        return deserialized;
    }

}
