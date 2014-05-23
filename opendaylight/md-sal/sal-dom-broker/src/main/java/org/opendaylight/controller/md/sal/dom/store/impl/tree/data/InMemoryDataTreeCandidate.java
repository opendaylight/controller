package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeCandidateNode;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.ModificationType;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.spi.TreeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

final class InMemoryDataTreeCandidate extends AbstractDataTreeCandidate {
    private static abstract class AbstractNode implements DataTreeCandidateNode {
        private final ModifiedNode mod;
        private final TreeNode newMeta;
        private final TreeNode oldMeta;

        protected AbstractNode(final ModifiedNode mod,
                final TreeNode oldMeta, final TreeNode newMeta) {
            this.newMeta = newMeta;
            this.oldMeta = oldMeta;
            this.mod = Preconditions.checkNotNull(mod);
        }

        protected final ModifiedNode getMod() {
            return mod;
        }

        protected final TreeNode getNewMeta() {
            return newMeta;
        }

        protected final TreeNode getOldMeta() {
            return oldMeta;
        }

        private static final TreeNode childMeta(final TreeNode parent, final PathArgument id) {
            if (parent != null) {
                return parent.getChild(id).orNull();
            } else {
                return null;
            }
        }

        @Override
        public Iterable<DataTreeCandidateNode> getChildNodes() {
            return Iterables.transform(mod.getChildren(), new Function<ModifiedNode, DataTreeCandidateNode>() {
                @Override
                public DataTreeCandidateNode apply(final ModifiedNode input) {
                    final PathArgument id = input.getIdentifier();
                    return new ChildNode(input, childMeta(oldMeta, id), childMeta(newMeta, id));
                }
            });
        }

        @Override
        public ModificationType getModificationType() {
            return mod.getType();
        }

        private Optional<NormalizedNode<?, ?>> optionalData(final TreeNode meta) {
            if (meta != null) {
                return Optional.<NormalizedNode<?,?>>of(meta.getData());
            } else {
                return Optional.absent();
            }
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
        public ChildNode(final ModifiedNode mod, final TreeNode oldMeta, final TreeNode newMeta) {
            super(mod, oldMeta, newMeta);
        }

        @Override
        public PathArgument getIdentifier() {
            return getMod().getIdentifier();
        }
    }

    private static final class RootNode extends AbstractNode {
        public RootNode(final ModifiedNode mod, final TreeNode oldMeta, final TreeNode newMeta) {
            super(mod, oldMeta, newMeta);
        }

        @Override
        public PathArgument getIdentifier() {
            throw new IllegalStateException("Attempted to get identifier of the root node");
        }
    }

    private final RootNode root;

    InMemoryDataTreeCandidate(final InstanceIdentifier rootPath, final ModifiedNode modificationRoot,
            final TreeNode beforeRoot, final TreeNode afterRoot) {
        super(rootPath);
        this.root = new RootNode(modificationRoot, beforeRoot, afterRoot);
    }

    TreeNode getAfterRoot() {
        return root.getNewMeta();
    }

    TreeNode getBeforeRoot() {
        return root.getOldMeta();
    }

    @Override
    public DataTreeCandidateNode getRootNode() {
        return root;
    }
}
