package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.spi.TreeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedLong;

/**
 * An implementation of apply operation which fails to do anything,
 * consistently. An instance of this class is used by the data tree
 * if it does not have a SchemaContext attached and hence cannot
 * perform anything meaningful.
 */
final class AlwaysFailOperation implements ModificationApplyOperation {
    @Override
    public Optional<TreeNode> apply(final ModifiedNode modification,
            final Optional<TreeNode> storeMeta, final UnsignedLong subtreeVersion) {
        throw new IllegalStateException("Schema Context is not available.");
    }

    @Override
    public void checkApplicable(final InstanceIdentifier path,final NodeModification modification, final Optional<TreeNode> storeMetadata) {
        throw new IllegalStateException("Schema Context is not available.");
    }

    @Override
    public Optional<ModificationApplyOperation> getChild(final PathArgument child) {
        throw new IllegalStateException("Schema Context is not available.");
    }

    @Override
    public void verifyStructure(final ModifiedNode modification) {
        throw new IllegalStateException("Schema Context is not available.");
    }
}