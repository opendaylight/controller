/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.spi.AbstractDOMDataTreeChangeListenerRegistration;
import org.opendaylight.controller.md.sal.dom.spi.AbstractRegistrationTree;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeNode;
import org.opendaylight.controller.md.sal.dom.spi.RegistrationTreeSnapshot;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for {@link DOMStoreTreeChangePublisher} implementations.
 */
public abstract class AbstractDOMStoreTreeChangePublisher extends AbstractRegistrationTree<AbstractDOMDataTreeChangeListenerRegistration<?>> implements DOMStoreTreeChangePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDOMStoreTreeChangePublisher.class);

    protected abstract void notifyRegistrations(YangInstanceIdentifier path, DataTreeCandidateNode candNode, Collection<AbstractDOMDataTreeChangeListenerRegistration<?>> registrations);
    protected abstract void registrationRemoved(AbstractDOMDataTreeChangeListenerRegistration<?> registration);

    protected final void notifyListeners(final DataTreeCandidate candidate) {
        final DataTreeCandidateNode node = candidate.getRootNode();
        if (node.getModificationType() == ModificationType.UNMODIFIED) {
            LOG.debug("Skipping unmodified candidate {}", candidate);
            return;
        }

        try (final RegistrationTreeSnapshot<AbstractDOMDataTreeChangeListenerRegistration<?>> snapshot = takeSnapshot()) {
            final List<PathArgument> toLookup = ImmutableList.copyOf(candidate.getRootPath().getPathArguments());
            lookupAndNotify(toLookup, 0, snapshot.getRootNode(), candidate);
        }
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(final YangInstanceIdentifier treeId, final L listener) {
        // Take the write lock
        takeLock();
        try {
            final RegistrationTreeNode<AbstractDOMDataTreeChangeListenerRegistration<?>> node = findNodeFor(treeId.getPathArguments());
            final AbstractDOMDataTreeChangeListenerRegistration<L> reg = new AbstractDOMDataTreeChangeListenerRegistration<L>(listener) {
                @Override
                protected void removeRegistration() {
                    AbstractDOMStoreTreeChangePublisher.this.removeRegistration(node, this);
                    registrationRemoved(this);
                }
            };

            addRegistration(node, reg);
            return reg;
        } finally {
            // Always release the lock
            releaseLock();
        }
    }

    private void lookupAndNotify(final List<PathArgument> args, final int offset, final RegistrationTreeNode<AbstractDOMDataTreeChangeListenerRegistration<?>> node, final DataTreeCandidate candidate) {
        if (args.size() != offset) {
            final PathArgument arg = args.get(offset);

            final RegistrationTreeNode<AbstractDOMDataTreeChangeListenerRegistration<?>> exactChild = node.getExactChild(arg);
            if (exactChild != null) {
                lookupAndNotify(args, offset + 1, exactChild, candidate);
            }

            for (RegistrationTreeNode<AbstractDOMDataTreeChangeListenerRegistration<?>> c : node.getInexactChildren(arg)) {
                lookupAndNotify(args, offset + 1, c, candidate);
            }
        } else {
            notifyNode(candidate.getRootPath(), node, candidate.getRootNode());
        }
    }

    private void notifyNode(final YangInstanceIdentifier path, final RegistrationTreeNode<AbstractDOMDataTreeChangeListenerRegistration<?>> regNode, final DataTreeCandidateNode candNode) {
        if (candNode.getModificationType() == ModificationType.UNMODIFIED) {
            LOG.debug("Skipping unmodified candidate {}", path);
            return;
        }

        final Collection<AbstractDOMDataTreeChangeListenerRegistration<?>> regs = regNode.getRegistrations();
        if (!regs.isEmpty()) {
            notifyRegistrations(path, candNode, regs);
        }

        for (DataTreeCandidateNode candChild : candNode.getChildNodes()) {
            if (candChild.getModificationType() != ModificationType.UNMODIFIED) {
                final RegistrationTreeNode<AbstractDOMDataTreeChangeListenerRegistration<?>> regChild = regNode.getExactChild(candChild.getIdentifier());
                if (regChild != null) {
                    notifyNode(path.node(candChild.getIdentifier()), regChild, candChild);
                }

                for (RegistrationTreeNode<AbstractDOMDataTreeChangeListenerRegistration<?>> rc : regNode.getInexactChildren(candChild.getIdentifier())) {
                    notifyNode(path.node(candChild.getIdentifier()), rc, candChild);
                }
            }
        }
    }
}
