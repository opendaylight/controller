/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import static org.opendaylight.controller.cluster.datastore.utils.ClusterUtils.PRODUCER_KEY_PREFIX_QNAME;
import static org.opendaylight.controller.cluster.datastore.utils.ClusterUtils.PRODUCER_MEMBER_QNAME;
import static org.opendaylight.controller.cluster.datastore.utils.ClusterUtils.PRODUCER_PREFIXES_QNAME;
import static org.opendaylight.controller.cluster.datastore.utils.ClusterUtils.PRODUCER_PREFIX_QNAME;

import akka.actor.ActorRef;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.sharding.messages.ProducerCreated;
import org.opendaylight.controller.cluster.sharding.messages.ProducerRemoved;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles updates to the producer status.
 */
public class ProducerStatusUpdateHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProducerStatusUpdateHandler.class);
    private final ActorRef handlingActor;
    private final MemberName memberName;

    private final EnumMap<LogicalDatastoreType,
            ListenerRegistration<DOMDataTreeChangeListener>> registrations =
            new EnumMap<>(LogicalDatastoreType.class);

    public ProducerStatusUpdateHandler(ActorRef handlingActor, MemberName memberName) {
        this.handlingActor = handlingActor;
        this.memberName = memberName;
    }

    public void initListener(final AbstractDataStore dataStore, final LogicalDatastoreType type) {
        registrations.put(type, dataStore.registerShardConfigListener(
                ClusterUtils.PRODUCER_LIST_PATH,
                new PrefixedShardConfigUpdateHandler.ShardConfigHandler(memberName, type, handlingActor)));
    }

    public void close() {
        registrations.values().forEach(ListenerRegistration::close);
        registrations.clear();
    }

    private static class ProducerStatusHandler implements ClusteredDOMDataTreeChangeListener {

        private final MemberName memberName;
        private final LogicalDatastoreType type;
        private final ActorRef handlingActor;
        private final String logName;

        ProducerStatusHandler(final MemberName memberName, final LogicalDatastoreType type,
                              final ActorRef handlingActor) {
            this.memberName = memberName;
            this.type = type;
            this.handlingActor = handlingActor;
            this.logName = memberName.getName() + "-" + type;
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

            LOG.debug("{}: New config received {}", logName, rootNode);
            LOG.debug("{}: Data after: {}", logName, rootNode.getDataAfter());

            // were in the producer list, iter children and resolve
            for (final DataTreeCandidateNode childNode : rootNode.getChildNodes()) {
                switch (childNode.getModificationType()) {
                    case UNMODIFIED:
                        break;
                    case SUBTREE_MODIFIED:
                    case APPEARED:
                    case WRITE:
                        resolveWrittenProducer(childNode);
                        break;
                    case DELETE:
                    case DISAPPEARED:
                        resolveDeletedProducer(childNode);
                        break;
                    default:
                        break;
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void resolveWrittenProducer(final DataTreeCandidateNode childNode) {
            final MapEntryNode entryNode = (MapEntryNode) childNode.getDataAfter().get();

            final LeafNode<String> member =
                    (LeafNode<String>) entryNode.getChild(new NodeIdentifier(PRODUCER_MEMBER_QNAME)).get();

            if (member.getValue().equals(memberName.getName())) {
                LOG.debug("Ignoring producer change: {} since it came from this node.", childNode);
                return;
            }

            final LeafNode<YangInstanceIdentifier> idPrefix =
                    (LeafNode<YangInstanceIdentifier>) entryNode.getChild(
                            new NodeIdentifier(PRODUCER_KEY_PREFIX_QNAME)).get();

            final ContainerNode prefixes =
                    (ContainerNode) entryNode.getChild(new NodeIdentifier(PRODUCER_PREFIXES_QNAME)).get();
            final LeafSetNode<YangInstanceIdentifier> prefixList =
                    (LeafSetNode<YangInstanceIdentifier>) prefixes.getChild(
                            new NodeIdentifier(PRODUCER_PREFIX_QNAME)).get();

            final Set<DOMDataTreeIdentifier> producerPrefixes =
                    prefixList.getValue().stream().map(leaf -> new DOMDataTreeIdentifier(type, leaf.getValue()))
                            .collect(Collectors.toSet());

            final ProducerCreated producerCreated =
                    new ProducerCreated(new DOMDataTreeIdentifier(type, idPrefix.getValue()), producerPrefixes);

            // assemble and send to the actor

            handlingActor.tell(producerCreated, ActorRef.noSender());

        }

        @SuppressWarnings("unchecked")
        private void resolveDeletedProducer(final DataTreeCandidateNode childNode) {
            final MapEntryNode entryNode = (MapEntryNode) childNode.getDataBefore().get();

            final LeafNode<String> member =
                    (LeafNode<String>) entryNode.getChild(new NodeIdentifier(PRODUCER_MEMBER_QNAME)).get();

            if (member.getValue().equals(memberName.getName())) {
                LOG.debug("Ignoring producer change: {} since it came from this node.", childNode);
                return;
            }

            final LeafNode<YangInstanceIdentifier> idPrefix =
                    (LeafNode<YangInstanceIdentifier>) entryNode.getChild(
                            new NodeIdentifier(PRODUCER_KEY_PREFIX_QNAME)).get();

            final ContainerNode prefixes =
                    (ContainerNode) entryNode.getChild(new NodeIdentifier(PRODUCER_PREFIXES_QNAME)).get();
            final LeafSetNode<YangInstanceIdentifier> prefixList =
                    (LeafSetNode<YangInstanceIdentifier>) prefixes.getChild(
                            new NodeIdentifier(PRODUCER_PREFIX_QNAME)).get();

            final Set<DOMDataTreeIdentifier> producerPrefixes =
                    prefixList.getValue().stream().map(leaf -> new DOMDataTreeIdentifier(type, leaf.getValue()))
                            .collect(Collectors.toSet());

            new ProducerRemoved(producerPrefixes);

            // assemble and send to the actor
        }

        private void resolveDelete(final DataTreeCandidateNode rootNode) {

        }

        @Override
        public String toString() {
            return "ShardConfigHandler [logName=" + logName + ", handlingActor=" + handlingActor + "]";
        }
    }

}