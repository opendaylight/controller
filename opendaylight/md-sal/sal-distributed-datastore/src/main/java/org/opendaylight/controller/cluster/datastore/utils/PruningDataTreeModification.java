/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.utils;

import com.google.common.base.Optional;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.opendaylight.controller.cluster.datastore.node.utils.transformer.NormalizedNodePruner;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The PruningDataTreeModification first removes all entries from the data which do not belong in the schemaContext
 * before delegating it to the actual DataTreeModification
 */
public class PruningDataTreeModification implements DataTreeModification {

    private static final Logger LOG = LoggerFactory.getLogger(PruningDataTreeModification.class);
    private final DataTreeModification delegate;
    private final SchemaContext schemaContext;
    private final Set<URI> validNamespaces;

    public PruningDataTreeModification(DataTreeModification delegate, SchemaContext schemaContext) {
        this.delegate = delegate;
        this.schemaContext = schemaContext;
        validNamespaces = new HashSet<>(schemaContext.getModules().size());
        for(org.opendaylight.yangtools.yang.model.api.Module module : schemaContext.getModules()){
            validNamespaces.add(module.getNamespace());
        }
    }

    @Override
    public void delete(YangInstanceIdentifier yangInstanceIdentifier) {
        delegate.delete(yangInstanceIdentifier);
    }

    @Override
    public void merge(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode<?, ?> normalizedNode) {
        try {
            delegate.merge(yangInstanceIdentifier, normalizedNode);
        } catch (IllegalArgumentException e){
            if(!isValidYangInstanceIdentifier(yangInstanceIdentifier)){
                return;
            }
            NormalizedNode<?,?> pruned = pruneNormalizedNode(normalizedNode);

            if(pruned != null) {
                delegate.write(yangInstanceIdentifier, pruned);
            }
        }

    }

    @Override
    public void write(YangInstanceIdentifier yangInstanceIdentifier, NormalizedNode<?, ?> normalizedNode) {
        try {
            delegate.write(yangInstanceIdentifier, normalizedNode);
        } catch (IllegalArgumentException e){
            if(!isValidYangInstanceIdentifier(yangInstanceIdentifier)){
                return;
            }

            NormalizedNode<?,?> pruned = pruneNormalizedNode(normalizedNode);

            if(pruned != null) {
                delegate.write(yangInstanceIdentifier, pruned);
            }
        }
    }

    @Override
    public void ready() {
        delegate.ready();
    }

    @Override
    public void applyToCursor(DataTreeModificationCursor dataTreeModificationCursor) {
        delegate.applyToCursor(dataTreeModificationCursor);
    }

    @Override
    public Optional<NormalizedNode<?, ?>> readNode(YangInstanceIdentifier yangInstanceIdentifier) {
        return delegate.readNode(yangInstanceIdentifier);
    }

    @Override
    public DataTreeModification newModification() {
        return new PruningDataTreeModification(delegate.newModification(), schemaContext);
    }

    private NormalizedNode<?, ?> pruneNormalizedNode(NormalizedNode<?,?> input){
        NormalizedNodePruner pruner = new NormalizedNodePruner(schemaContext);
        try {
            NormalizedNodeWriter.forStreamWriter(pruner).write(input);
        } catch (IOException ioe) {
            LOG.error("Unexpected IOException when pruning normalizedNode");
        }

        return pruner.normalizedNode();
    }

    public DataTreeModification getDelegate(){
        return delegate;
    }

    private boolean isValidYangInstanceIdentifier(YangInstanceIdentifier instanceIdentifier){
        for(YangInstanceIdentifier.PathArgument pathArgument : instanceIdentifier.getPathArguments()){
            if(!validNamespaces.contains(pathArgument.getNodeType().getNamespace())){
                return false;
            }
        }

        return true;
    }

}
