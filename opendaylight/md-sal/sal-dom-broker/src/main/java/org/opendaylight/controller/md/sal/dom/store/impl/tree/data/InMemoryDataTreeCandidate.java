package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeCandidateNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

final class InMemoryDataTreeCandidate extends AbstractDataTreeCandidate {
    private static abstract class AbstractNode implements DataTreeCandidateNode {
        private final StoreMetadataNode newMeta;
        private final StoreMetadataNode oldMeta;
        private final NodeModification mod;

        protected AbstractNode(final NodeModification mod,
                final StoreMetadataNode oldMeta, final StoreMetadataNode newMeta) {
            this.newMeta = newMeta;
            this.oldMeta = oldMeta;
            this.mod = Preconditions.checkNotNull(mod);
        }

        protected final NodeModification getMod() {
            return mod;
        }

        protected final StoreMetadataNode getNewMeta() {
            return newMeta;
        }

        protected final StoreMetadataNode getOldMeta() {
            return oldMeta;
        }

        private static final StoreMetadataNode childMeta(final StoreMetadataNode parent, final PathArgument id) {
            return parent == null ? null : parent.getChild(id).orNull();
        }

        @Override
        public Iterable<DataTreeCandidateNode> getChildNodes() {
            return Iterables.transform(mod.getModifications(), new Function<NodeModification, DataTreeCandidateNode>() {
                @Override
                public DataTreeCandidateNode apply(final NodeModification input) {
                    final PathArgument id = input.getIdentifier();
                    return new ChildNode(input, childMeta(oldMeta, id), childMeta(newMeta, id));
                }
            });
        }

        @Override
        public ModificationType getModificationType() {
            return mod.getModificationType();
        }

        private Optional<NormalizedNode<?, ?>> optionalData(StoreMetadataNode meta) {
            if (meta == null) {
                return Optional.absent();
            }
            return Optional.<NormalizedNode<?,?>>of(meta.getData());
        }

        @Override
        public Optional<NormalizedNode<?, ?>> getDataAfter() {
            return optionalData(newMeta);
        }

        @Override
        public Optional<NormalizedNode<?, ?>> getDataBefore() {
            return optionalData(oldMeta);
        }
    }

    private static final class ChildNode extends AbstractNode {
        public ChildNode(NodeModification mod, StoreMetadataNode oldMeta, StoreMetadataNode newMeta) {
            super(mod, oldMeta, newMeta);
        }

        @Override
        public PathArgument getIdentifier() {
            return getMod().getIdentifier();
        }
    }

    private static final class RootNode extends AbstractNode {
        public RootNode(NodeModification mod, StoreMetadataNode oldMeta, StoreMetadataNode newMeta) {
            super(mod, oldMeta, newMeta);
        }

        @Override
        public PathArgument getIdentifier() {
            throw new IllegalStateException("Attempted to get identifier of the root node");
        }
    }

    private final RootNode root;

    InMemoryDataTreeCandidate(final InstanceIdentifier rootPath, final NodeModification modificationRoot,
            final StoreMetadataNode oldRoot, final StoreMetadataNode newRoot) {
        super(rootPath);
        this.root = new RootNode(modificationRoot, oldRoot, newRoot);
    }

    StoreMetadataNode getAfterRoot() {
        return root.getNewMeta();
    }

    StoreMetadataNode getBeforeRoot() {
        return root.getOldMeta();
    }

    @Override
    public DataTreeCandidateNode getRootNode() {
        return root;
    }
}
