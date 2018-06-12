/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.collect.ImmutableBiMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTreeNode;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS", justification = "Migration path")
public class BindingToNormalizedNodeCodec
        extends org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec {

    private DataNormalizer legacyToNormalized = null;

    public BindingToNormalizedNodeCodec(final ClassLoadingStrategy classLoadingStrategy,
            final BindingNormalizedNodeCodecRegistry codecRegistry) {
        super(classLoadingStrategy, codecRegistry);
    }

    public BindingToNormalizedNodeCodec(final ClassLoadingStrategy classLoadingStrategy,
            final BindingNormalizedNodeCodecRegistry codecRegistry, final boolean waitForSchema) {
        super(classLoadingStrategy, codecRegistry, waitForSchema);
    }

    DataNormalizer getDataNormalizer() {
        return this.legacyToNormalized;
    }

    @Override
    public YangInstanceIdentifier toYangInstanceIdentifierBlocking(
            final InstanceIdentifier<? extends DataObject> binding) {
        return super.toYangInstanceIdentifierBlocking(binding);
    }

    @Override
    public YangInstanceIdentifier toYangInstanceIdentifierCached(final InstanceIdentifier<?> binding) {
        return super.toYangInstanceIdentifierCached(binding);
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext schemaContext) {
        this.legacyToNormalized = new DataNormalizer(schemaContext);
        super.onGlobalContextUpdated(schemaContext);
    }

    /**
     * Returns an default object according to YANG schema for supplied path.
     *
     * @param path DOM Path
     * @return Node with defaults set on.
     */
    @Override
    public NormalizedNode<?, ?> getDefaultNodeFor(final YangInstanceIdentifier path) {
        final Iterator<PathArgument> iterator = path.getPathArguments().iterator();
        DataNormalizationOperation<?> currentOp = this.legacyToNormalized.getRootOperation();
        while (iterator.hasNext()) {
            final PathArgument currentArg = iterator.next();
            try {
                currentOp = currentOp.getChild(currentArg);
            } catch (final DataNormalizationException e) {
                throw new IllegalArgumentException(String.format("Invalid child encountered in path %s", path), e);
            }
        }
        return currentOp.createDefault(path.getLastPathArgument());
    }

    @Override
    public ImmutableBiMap<Method, RpcDefinition> getRpcMethodToSchema(final Class<? extends RpcService> key) {
        return super.getRpcMethodToSchema(key);
    }

    @Override
    @Nonnull
    public Map.Entry<InstanceIdentifier<?>, BindingCodecTreeNode<?>> getSubtreeCodec(
            final YangInstanceIdentifier domIdentifier) {
        return super.getSubtreeCodec(domIdentifier);
    }
}
