/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import akka.japi.Creator;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChanged;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.EnableNotification;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DataTreeChangeListener extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(DataChangeListener.class);
    private final DOMDataTreeChangeListener listener;
    private boolean notificationsEnabled = false;
    private DataTree dataTree;
    private SchemaContext context;
    private DataNormalizer normalizer;

    private DataTreeChangeListener(final SchemaContext context, final DOMDataTreeChangeListener listener) {
        this.listener = Preconditions.checkNotNull(listener);
        this.context = Preconditions.checkNotNull(context);
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        if (message instanceof DataTreeChanged) {
            dataChanged((DataTreeChanged)message);
        } else if (message instanceof EnableNotification) {
            enableNotification((EnableNotification) message);
        } else if (message instanceof UpdateSchemaContext) {
            updateSchemaContext((UpdateSchemaContext) message);
        }
    }

    private void updateSchemaContext(final UpdateSchemaContext message) {
        if (notificationsEnabled) {
            context = message.getSchemaContext();
            dataTree.setSchemaContext(context);
            normalizer = new DataNormalizer(context);
        }
    }

    private static void applyNode(final DataTreeModification tx, final YangInstanceIdentifier path, final DataTreeCandidateNode node) {
        switch (node.getModificationType()) {
        case DELETE:
            tx.delete(path);
            break;
        case SUBTREE_MODIFIED:
            for (DataTreeCandidateNode child : node.getChildNodes()) {
                applyNode(tx, path.node(child.getIdentifier()), child);
            }
            break;
        case UNMODIFIED:
            // No-op
            break;
        case WRITE:
            tx.write(path, node.getDataAfter().get());
            break;
        default:
            throw new IllegalArgumentException("Unsupported modification " + node.getModificationType());
        }
    }

    // the APIs used by this method will be resurrected somewhere else
    @SuppressWarnings("deprecation")
    private void ensureParentsByMerge(final DataTreeModification tx, final YangInstanceIdentifier path) {
        if (!tx.readNode(path).isPresent()) {
            List<PathArgument> currentArguments = new ArrayList<>();
            DataNormalizationOperation<?> currentOp = normalizer.getRootOperation();

            // FIXME: create a utility which will perform a merge in one go. Also note that we do NOT
            //        create the ultimate entry, to preserve correctness of the change operation.

            final Iterator<PathArgument> arg = path.getPathArguments().iterator();
            while (arg.hasNext()) {
                final PathArgument currentArg = arg.next();
                try {
                    currentOp = currentOp.getChild(currentArg);
                } catch (DataNormalizationException e) {
                    throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
                }
                currentArguments.add(currentArg);
                YangInstanceIdentifier currentPath = YangInstanceIdentifier.create(currentArguments);

                if (arg.hasNext()) {
                    tx.merge(currentPath, currentOp.createDefault(currentArg));
                }
            }
        }
    }

    private void dataChanged(final DataTreeChanged message) {
        // Do nothing if notifications are not enabled
        if (!notificationsEnabled) {
            LOG.debug("Notifications not enabled for listener {} - dropping change notification", listener);
            return;
        }

        // FIXME: this may not be needed for local-only listeners, but what will happen
        //        if the leader moves away and we suddenly find ourselves in need to replay?
        final Collection<DataTreeCandidate> incompleteChanges = message.getChanges();
        final Collection<DataTreeCandidate> completeChanges = new ArrayList<>(incompleteChanges.size());
        for (final DataTreeCandidate ic : incompleteChanges) {
            // FIXME: if we move the snapshot out of the loop, we can do state compression,
            //        but then have to split it into individual roots.
            final DataTreeModification tx = dataTree.takeSnapshot().newModification();

            ensureParentsByMerge(tx, ic.getRootPath());

            // Now apply the operation
            applyNode(tx, ic.getRootPath(), ic.getRootNode());

            try {
                dataTree.validate(tx);
            } catch (DataValidationFailedException e) {
                LOG.error("Failed to validate changeset {}", ic, e);
                continue;
            }

            final DataTreeCandidate cc = dataTree.prepare(tx);
            dataTree.commit(cc);

            // We need to trim this back to the root that was reported. If we maintain
            // data trees per root, this would be solved (but more smarts need to be applied
            // for state compression thing above.
            DataTreeCandidateNode wlk = cc.getRootNode();
            for (PathArgument arg : ic.getRootPath().getPathArguments()) {
                final DataTreeCandidateNode child = wlk.getModifiedChild(arg);
                Verify.verify(child != null, "Failed to find %s in %s", ic.getRootPath(), cc);
                wlk = child;
            }

            completeChanges.add(new DataTreeCandidateImpl(ic.getRootPath(), wlk));
        }

        LOG.debug("Sending change notification {} to listener {}", completeChanges, listener);

        try {
            this.listener.onDataTreeChanged(completeChanges);
        } catch (RuntimeException e) {
            LOG.error("Error notifying listener {}", this.listener, e);
        }

        // TODO: do we really need this?
        // It seems the sender is never null but it doesn't hurt to check. If the caller passes in
        // a null sender (ActorRef.noSender()), akka translates that to the deadLetters actor.
        if (getSender() != null && !getContext().system().deadLetters().equals(getSender())) {
            getSender().tell(DataTreeChangedReply.getInstance(), getSelf());
        }
    }

    private void enableNotification(final EnableNotification message) {
        notificationsEnabled = message.isEnabled();

        if (notificationsEnabled) {
            dataTree = InMemoryDataTreeFactory.getInstance().create();
            dataTree.setSchemaContext(context);
            normalizer = new DataNormalizer(context);
        } else {
            dataTree = null;
            normalizer = null;
        }

        LOG.debug("{} notifications for listener {}", (notificationsEnabled ? "Enabled" : "Disabled"),
                listener);
    }

    public static Props props(final SchemaContext context, final DOMDataTreeChangeListener listener) {
        return Props.create(new DataTreeChangeListenerCreator(context, listener));
    }

    private static final class DataTreeCandidateImpl implements DataTreeCandidate {
        private final YangInstanceIdentifier rootPath;
        private final DataTreeCandidateNode rootNode;

        DataTreeCandidateImpl(final YangInstanceIdentifier rootPath, final DataTreeCandidateNode rootNode) {
            this.rootPath = Preconditions.checkNotNull(rootPath);
            this.rootNode = Preconditions.checkNotNull(rootNode);
        }

        @Override
        public DataTreeCandidateNode getRootNode() {
            return rootNode;
        }

        @Override
        public YangInstanceIdentifier getRootPath() {
            return rootPath;
        }
    }

    private static class DataTreeChangeListenerCreator implements Creator<DataTreeChangeListener> {
        private static final long serialVersionUID = 1L;
        private final DOMDataTreeChangeListener listener;
        private final SchemaContext context;

        DataTreeChangeListenerCreator(final SchemaContext context, final DOMDataTreeChangeListener listener) {
            this.listener = Preconditions.checkNotNull(listener);
            this.context = Preconditions.checkNotNull(context);
        }

        @Override
        public DataTreeChangeListener create() {
            return new DataTreeChangeListener(context, listener);
        }
    }
}
