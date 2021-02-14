/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.AbstractDOMDataTreeChangeListenerRegistration;
import org.opendaylight.mdsal.dom.spi.AbstractRegistrationTree;
import org.opendaylight.mdsal.dom.spi.RegistrationTreeNode;
import org.opendaylight.mdsal.dom.spi.shard.ChildShardContext;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNodes;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidates;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.SchemaValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(forRemoval = true)
public class DistributedShardChangePublisher
        extends AbstractRegistrationTree<AbstractDOMDataTreeChangeListenerRegistration<?>>
        implements DOMStoreTreeChangePublisher {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardChangePublisher.class);

    private final DistributedDataStoreInterface distributedDataStore;
    private final YangInstanceIdentifier shardPath;

    private final Map<DOMDataTreeIdentifier, ChildShardContext> childShards;

    @GuardedBy("this")
    private final DataTree dataTree;

    public DistributedShardChangePublisher(final DataStoreClient client,
                                           final DistributedDataStoreInterface distributedDataStore,
                                           final DOMDataTreeIdentifier prefix,
                                           final Map<DOMDataTreeIdentifier, ChildShardContext> childShards) {
        this.distributedDataStore = distributedDataStore;
        // TODO keeping the whole dataTree thats contained in subshards doesn't seem like a good idea
        // maybe the whole listener logic would be better in the backend shards where we have direct access to the
        // dataTree and wont have to cache it redundantly.

        final DataTreeConfiguration baseConfig;
        switch (prefix.getDatastoreType()) {
            case CONFIGURATION:
                baseConfig = DataTreeConfiguration.DEFAULT_CONFIGURATION;
                break;
            case OPERATIONAL:
                baseConfig = DataTreeConfiguration.DEFAULT_OPERATIONAL;
                break;
            default:
                throw new UnsupportedOperationException("Unknown prefix type " + prefix.getDatastoreType());
        }

        this.dataTree = new InMemoryDataTreeFactory().create(new DataTreeConfiguration.Builder(baseConfig.getTreeType())
                .setMandatoryNodesValidation(baseConfig.isMandatoryNodesValidationEnabled())
                .setUniqueIndexes(baseConfig.isUniqueIndexEnabled())
                .setRootPath(prefix.getRootIdentifier())
                .build());

        // XXX: can we guarantee that the root is present in the schemacontext?
        this.dataTree.setEffectiveModelContext(distributedDataStore.getActorUtils().getSchemaContext());
        this.shardPath = prefix.getRootIdentifier();
        this.childShards = childShards;
    }

    protected void registrationRemoved(final AbstractDOMDataTreeChangeListenerRegistration<?> registration) {
        LOG.debug("Closing registration {}", registration);
    }

    @Override
    public <L extends DOMDataTreeChangeListener> AbstractDOMDataTreeChangeListenerRegistration<L>
            registerTreeChangeListener(final YangInstanceIdentifier path, final L listener) {
        takeLock();
        try {
            return setupListenerContext(path, listener);
        } finally {
            releaseLock();
        }
    }

    private <L extends DOMDataTreeChangeListener> AbstractDOMDataTreeChangeListenerRegistration<L>
            setupListenerContext(final YangInstanceIdentifier listenerPath, final L listener) {
        // we need to register the listener registration path based on the shards root
        // we have to strip the shard path from the listener path and then register
        YangInstanceIdentifier strippedIdentifier = listenerPath;
        if (!shardPath.isEmpty()) {
            strippedIdentifier = YangInstanceIdentifier.create(stripShardPath(shardPath, listenerPath));
        }

        final DOMDataTreeListenerWithSubshards subshardListener =
                new DOMDataTreeListenerWithSubshards(strippedIdentifier, listener);
        final AbstractDOMDataTreeChangeListenerRegistration<L> reg =
                setupContextWithoutSubshards(listenerPath, strippedIdentifier, subshardListener);

        for (final ChildShardContext maybeAffected : childShards.values()) {
            if (listenerPath.contains(maybeAffected.getPrefix().getRootIdentifier())) {
                // consumer has initialDataChangeEvent subshard somewhere on lower level
                // register to the notification manager with snapshot and forward child notifications to parent
                LOG.debug("Adding new subshard{{}} to listener at {}", maybeAffected.getPrefix(), listenerPath);
                subshardListener.addSubshard(maybeAffected);
            } else if (maybeAffected.getPrefix().getRootIdentifier().contains(listenerPath)) {
                // bind path is inside subshard
                // TODO can this happen? seems like in ShardedDOMDataTree we are
                // already registering to the lowest shard possible
                throw new UnsupportedOperationException("Listener should be registered directly "
                        + "into initialDataChangeEvent subshard");
            }
        }

        return reg;
    }

    private <L extends DOMDataTreeChangeListener> AbstractDOMDataTreeChangeListenerRegistration<L>
            setupContextWithoutSubshards(final YangInstanceIdentifier shardLookup,
                                         final YangInstanceIdentifier listenerPath,
                                         final DOMDataTreeListenerWithSubshards listener) {

        LOG.debug("Registering root listener full path: {}, path inside shard: {}", shardLookup, listenerPath);

        // register in the shard tree
        final RegistrationTreeNode<AbstractDOMDataTreeChangeListenerRegistration<?>> node =
                findNodeFor(listenerPath.getPathArguments());

        // register listener in CDS
        ListenerRegistration<DOMDataTreeChangeListener> listenerReg = distributedDataStore
                .registerProxyListener(shardLookup, listenerPath, listener);

        @SuppressWarnings("unchecked")
        final AbstractDOMDataTreeChangeListenerRegistration<L> registration =
            new AbstractDOMDataTreeChangeListenerRegistration<>((L) listener) {
                @Override
                protected void removeRegistration() {
                    listener.close();
                    DistributedShardChangePublisher.this.removeRegistration(node, this);
                    registrationRemoved(this);
                    listenerReg.close();
                }
            };
        addRegistration(node, registration);

        return registration;
    }

    private static Iterable<PathArgument> stripShardPath(final YangInstanceIdentifier shardPath,
                                                         final YangInstanceIdentifier listenerPath) {
        if (shardPath.isEmpty()) {
            return listenerPath.getPathArguments();
        }

        final List<PathArgument> listenerPathArgs = new ArrayList<>(listenerPath.getPathArguments());
        final Iterator<PathArgument> shardIter = shardPath.getPathArguments().iterator();
        final Iterator<PathArgument> listenerIter = listenerPathArgs.iterator();

        while (shardIter.hasNext()) {
            if (shardIter.next().equals(listenerIter.next())) {
                listenerIter.remove();
            } else {
                break;
            }
        }

        return listenerPathArgs;
    }

    synchronized DataTreeCandidate applyChanges(final YangInstanceIdentifier listenerPath,
            final Collection<DataTreeCandidate> changes) throws DataValidationFailedException {
        final DataTreeModification modification = dataTree.takeSnapshot().newModification();
        for (final DataTreeCandidate change : changes) {
            try {
                DataTreeCandidates.applyToModification(modification, change);
            } catch (SchemaValidationFailedException e) {
                LOG.error("Validation failed", e);
            }
        }

        modification.ready();

        final DataTreeCandidate candidate;

        dataTree.validate(modification);

        // strip nodes we dont need since this listener doesn't have to be registered at the root of the DataTree
        candidate = dataTree.prepare(modification);
        dataTree.commit(candidate);


        DataTreeCandidateNode modifiedChild = candidate.getRootNode();

        for (final PathArgument pathArgument : listenerPath.getPathArguments()) {
            modifiedChild = modifiedChild.getModifiedChild(pathArgument).orElse(null);
        }


        if (modifiedChild == null) {
            modifiedChild = DataTreeCandidateNodes.empty(dataTree.getRootPath().getLastPathArgument());
        }

        return DataTreeCandidates.newDataTreeCandidate(dataTree.getRootPath(), modifiedChild);
    }


    private final class DOMDataTreeListenerWithSubshards implements DOMDataTreeChangeListener {

        private final YangInstanceIdentifier listenerPath;
        private final DOMDataTreeChangeListener delegate;
        private final Map<YangInstanceIdentifier, ListenerRegistration<DOMDataTreeChangeListener>> registrations =
                new ConcurrentHashMap<>();

        @GuardedBy("this")
        private final Collection<DataTreeCandidate> stashedDataTreeCandidates = new LinkedList<>();

        DOMDataTreeListenerWithSubshards(final YangInstanceIdentifier listenerPath,
                                         final DOMDataTreeChangeListener delegate) {
            this.listenerPath = requireNonNull(listenerPath);
            this.delegate = requireNonNull(delegate);
        }

        @Override
        public synchronized void onDataTreeChanged(final Collection<DataTreeCandidate> changes) {
            LOG.debug("Received data changed {}", changes);

            if (!stashedDataTreeCandidates.isEmpty()) {
                LOG.debug("Adding stashed subshards' changes {}", stashedDataTreeCandidates);
                changes.addAll(stashedDataTreeCandidates);
                stashedDataTreeCandidates.clear();
            }

            try {
                applyChanges(listenerPath, changes);
            } catch (final DataValidationFailedException e) {
                // TODO should we fail here? What if stashed changes
                // (changes from subshards) got ahead more than one generation
                // from current shard. Than we can fail to apply this changes
                // upon current data tree, but once we get respective changes
                // from current shard, we can apply also changes from
                // subshards.
                //
                // However, we can loose ability to notice and report some
                // errors then. For example, we cannot detect potential lost
                // changes from current shard.
                LOG.error("Validation failed for modification built from changes {}, current data tree: {}",
                        changes, dataTree, e);
                throw new RuntimeException("Notification validation failed", e);
            }

            delegate.onDataTreeChanged(changes);
        }

        synchronized void onDataTreeChanged(final YangInstanceIdentifier pathFromRoot,
                                            final Collection<DataTreeCandidate> changes) {
            final YangInstanceIdentifier changeId =
                    YangInstanceIdentifier.create(stripShardPath(dataTree.getRootPath(), pathFromRoot));

            final List<DataTreeCandidate> newCandidates = changes.stream()
                    .map(candidate -> DataTreeCandidates.newDataTreeCandidate(changeId, candidate.getRootNode()))
                    .collect(Collectors.toList());

            try {
                delegate.onDataTreeChanged(Collections.singleton(applyChanges(listenerPath, newCandidates)));
            } catch (final DataValidationFailedException e) {
                // We cannot apply changes from subshard to current data tree.
                // Maybe changes from current shard haven't been applied to
                // data tree yet. Postpone processing of these changes till we
                // receive changes from current shard.
                LOG.debug("Validation for modification built from subshard {} changes {} failed, current data tree {}.",
                        pathFromRoot, changes, dataTree, e);
                stashedDataTreeCandidates.addAll(newCandidates);
            }
        }

        void addSubshard(final ChildShardContext context) {
            checkState(context.getShard() instanceof DOMStoreTreeChangePublisher,
                    "All subshards that are initialDataChangeEvent part of ListenerContext need to be listenable");

            final DOMStoreTreeChangePublisher listenableShard = (DOMStoreTreeChangePublisher) context.getShard();
            // since this is going into subshard we want to listen for ALL changes in the subshard
            registrations.put(context.getPrefix().getRootIdentifier(),
                    listenableShard.registerTreeChangeListener(
                            context.getPrefix().getRootIdentifier(), changes -> onDataTreeChanged(
                                    context.getPrefix().getRootIdentifier(), changes)));
        }

        void close() {
            for (final ListenerRegistration<DOMDataTreeChangeListener> registration : registrations.values()) {
                registration.close();
            }
            registrations.clear();
        }
    }
}
