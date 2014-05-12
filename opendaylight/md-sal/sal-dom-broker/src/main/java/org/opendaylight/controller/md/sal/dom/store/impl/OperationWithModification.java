/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.NodeModification;
import org.opendaylight.controller.md.sal.dom.store.impl.tree.StoreMetadataNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
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

    public NodeModification getModification() {
        return modification;
    }

    public ModificationApplyOperation getApplyOperation() {
        return applyOperation;
    }

    public Optional<StoreMetadataNode> apply(final Optional<StoreMetadataNode> data, final UnsignedLong subtreeVersion) {
        return applyOperation.apply(modification, data, subtreeVersion);
    }

    public static OperationWithModification from(final ModificationApplyOperation operation,
            final NodeModification modification) {
        return new OperationWithModification(operation, modification);

    }

    public void merge(final NormalizedNode<?, ?> data) {
        modification.merge(data);
        applyOperation.verifyStructure(modification);

    }

    public OperationWithModification forChild(final PathArgument childId) {
        NodeModification childMod = modification.modifyChild(childId);
        Optional<ModificationApplyOperation> childOp = applyOperation.getChild(childId);
        return from(childOp.get(),childMod);
    }
}
