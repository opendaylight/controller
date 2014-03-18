package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedLong;

public class OperationWithModification {

    private final NodeModification modification;
    private final ModificationApplyOperation applyOperation;

    private OperationWithModification(final ModificationApplyOperation op, final NodeModification mod) {
        this.modification = mod;
        this.applyOperation = op;
    }

    public OperationWithModification write(final NormalizedNode<?, ?> value) {
        modification.write(value);
        applyOperation.verifyStructure(modification);
        return this;
    }

    public OperationWithModification delete() {
        modification.delete();
        return this;
    }

    public boolean isApplicable(final Optional<StoreMetadataNode> data) {
        return applyOperation.isApplicable(modification, data);
    }

    public Optional<StoreMetadataNode> apply(final Optional<StoreMetadataNode> data, final UnsignedLong subtreeVersion) {
        return applyOperation.apply(modification, data, subtreeVersion);
    }

    public static OperationWithModification from(final ModificationApplyOperation operation,
            final NodeModification modification) {
        return new OperationWithModification(operation, modification);

    }
}