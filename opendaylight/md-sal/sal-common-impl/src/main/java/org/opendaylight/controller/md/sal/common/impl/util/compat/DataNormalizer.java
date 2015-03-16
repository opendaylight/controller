/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.util.compat;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @deprecated This class provides compatibility between XML semantics
 * and {@link org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree}
 */
@Deprecated
public class DataNormalizer {

    private final DataNormalizationOperation<?> operation;

    public DataNormalizer(final SchemaContext ctx) {
        operation = DataNormalizationOperation.from(ctx);
    }

    public YangInstanceIdentifier toNormalized(final YangInstanceIdentifier legacy) {
        ImmutableList.Builder<PathArgument> normalizedArgs = ImmutableList.builder();

        DataNormalizationOperation<?> currentOp = operation;
        Iterator<PathArgument> arguments = legacy.getPathArguments().iterator();

        try {
            while (arguments.hasNext()) {
                PathArgument legacyArg = arguments.next();
                currentOp = currentOp.getChild(legacyArg);
                checkArgument(currentOp != null,
                        "Legacy Instance Identifier %s is not correct. Normalized Instance Identifier so far %s",
                        legacy, normalizedArgs.build());
                while (currentOp.isMixin()) {
                    normalizedArgs.add(currentOp.getIdentifier());
                    currentOp = currentOp.getChild(legacyArg.getNodeType());
                }
                normalizedArgs.add(legacyArg);
            }
        } catch (DataNormalizationException e) {
            throw new IllegalArgumentException(String.format("Failed to normalize path %s", legacy), e);
        }

        return YangInstanceIdentifier.create(normalizedArgs.build());
    }

    public DataNormalizationOperation<?> getOperation(final YangInstanceIdentifier legacy) throws DataNormalizationException {
        DataNormalizationOperation<?> currentOp = operation;
        Iterator<PathArgument> arguments = legacy.getPathArguments().iterator();

        while (arguments.hasNext()) {
            currentOp = currentOp.getChild(arguments.next());
        }
        return currentOp;
    }

    public YangInstanceIdentifier toLegacy(final YangInstanceIdentifier normalized) throws DataNormalizationException {
        ImmutableList.Builder<PathArgument> legacyArgs = ImmutableList.builder();
        DataNormalizationOperation<?> currentOp = operation;
        for (PathArgument normalizedArg : normalized.getPathArguments()) {
            currentOp = currentOp.getChild(normalizedArg);
            if (!currentOp.isMixin()) {
                legacyArgs.add(normalizedArg);
            }
        }
        return YangInstanceIdentifier.create(legacyArgs.build());
    }

    public DataNormalizationOperation<?> getRootOperation() {
        return operation;
    }

}
