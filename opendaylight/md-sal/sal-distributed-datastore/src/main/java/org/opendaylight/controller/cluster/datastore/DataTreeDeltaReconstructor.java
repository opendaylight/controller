/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeChangedReply;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeDelta;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

final class DataTreeDeltaReconstructor extends AbstractUntypedActor {
    private final DataTree dataTree = InMemoryDataTreeFactory.getInstance().create();
    private DataNormalizer normalizer;
    private SchemaContext context;

    private DataTreeDeltaReconstructor(final SchemaContext context) {
        this.context = Preconditions.checkNotNull(context);
        dataTree.setSchemaContext(context);
        normalizer = new DataNormalizer(context);
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        if (message instanceof DataTreeDelta) {
            reconstructDelta((DataTreeDelta)message);
        } else if (message instanceof UpdateSchemaContext) {
            updateSchemaContext((UpdateSchemaContext) message);
        }
    }

    private void reconstructDelta(final DataTreeDelta message) {
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

        // FIXME: We've got the registration, time to redistribute to local actors

        // TODO: do we really need this?
        // It seems the sender is never null but it doesn't hurt to check. If the caller passes in
        // a null sender (ActorRef.noSender()), akka translates that to the deadLetters actor.
        if (getSender() != null && !getContext().system().deadLetters().equals(getSender())) {
            getSender().tell(DataTreeChangedReply.getInstance(), getSelf());
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

    private void updateSchemaContext(final UpdateSchemaContext message) {
        context = message.getSchemaContext();
        dataTree.setSchemaContext(context);
        normalizer = new DataNormalizer(context);
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
}
