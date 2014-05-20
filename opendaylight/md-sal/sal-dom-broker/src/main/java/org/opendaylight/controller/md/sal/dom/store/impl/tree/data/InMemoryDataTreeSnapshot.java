package org.opendaylight.controller.md.sal.dom.store.impl.tree.data;

import org.opendaylight.controller.md.sal.dom.store.impl.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeUtils;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

final class InMemoryDataTreeSnapshot implements DataTreeSnapshot {
    private final ModificationApplyOperation applyOper;
    private final SchemaContext schemaContext;
    private final StoreMetadataNode rootNode;

    InMemoryDataTreeSnapshot(final SchemaContext schemaContext, final StoreMetadataNode rootNode,
            final ModificationApplyOperation applyOper) {
        this.schemaContext = Preconditions.checkNotNull(schemaContext);
        this.rootNode = Preconditions.checkNotNull(rootNode);
        this.applyOper = Preconditions.checkNotNull(applyOper);
    }

    StoreMetadataNode getRootNode() {
        return rootNode;
    }

    SchemaContext getSchemaContext() {
        return schemaContext;
    }

    @Override
    public Optional<NormalizedNode<?, ?>> readNode(final InstanceIdentifier path) {
        return NormalizedNodeUtils.findNode(rootNode.getData(), path);
    }

    @Override
    public InMemoryDataTreeModification newModification() {
        return new InMemoryDataTreeModification(this, applyOper);
    }

    @Override
    public String toString() {
        return rootNode.getSubtreeVersion().toString();
    }

}