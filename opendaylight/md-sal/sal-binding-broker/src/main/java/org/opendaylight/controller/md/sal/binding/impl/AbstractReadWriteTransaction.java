/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AbstractReadWriteTransaction extends AbstractWriteTransaction<DOMDataReadWriteTransaction> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractReadWriteTransaction.class);

    public AbstractReadWriteTransaction(final DOMDataReadWriteTransaction delegate, final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

    @Override
    protected final void ensureParentsByMerge(final LogicalDatastoreType store,
            final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier normalizedPath,
            final InstanceIdentifier<?> path) {
        List<PathArgument> currentArguments = new ArrayList<>();
        DataNormalizationOperation<?> currentOp = getCodec().getDataNormalizer().getRootOperation();
        Iterator<PathArgument> iterator = normalizedPath.getPathArguments().iterator();
        while (iterator.hasNext()) {
            PathArgument currentArg = iterator.next();
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
            }
            currentArguments.add(currentArg);
            org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier currentPath = org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.create(
                    currentArguments);

            final Boolean exists;
            try {
                exists = getDelegate().exists(store, currentPath).checkedGet();
            } catch (ReadFailedException e) {
                LOG.error("Failed to read pre-existing data from store {} path {}", store, currentPath, e);
                throw new IllegalStateException("Failed to read pre-existing data", e);
            }

            if (!exists && iterator.hasNext()) {
                getDelegate().merge(store, currentPath, currentOp.createDefault(currentArg));
            }
        }
    }


}
