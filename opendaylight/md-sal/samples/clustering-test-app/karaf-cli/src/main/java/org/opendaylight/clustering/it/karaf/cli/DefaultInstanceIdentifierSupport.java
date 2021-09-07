/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.clustering.it.karaf.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.Optional;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingCodecTree;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingInstanceIdentifierCodec;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.util.codec.TypeAwareCodec;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeAware;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;

@Component
@RequireServiceComponentRuntime
public final class DefaultInstanceIdentifierSupport implements InstanceIdentifierSupport {
    private final BindingInstanceIdentifierCodec bindingCodec;
    private final TypeAwareCodec<?, ?, ?> jsonCodec;

    @Activate
    public DefaultInstanceIdentifierSupport(@Reference final BindingCodecTree bindingCodecTree,
            @Reference final BindingRuntimeContext runtimeContext) {
        bindingCodec = bindingCodecTree.getInstanceIdentifierCodec();
        jsonCodec = JSONCodecFactorySupplier.RFC7951.createLazy(runtimeContext.getEffectiveModelContext())
            .codecFor(new FakeLeafDefinition(), null);
    }

    @Override
    public InstanceIdentifier<?> parseArgument(final String argument) {
        final YangInstanceIdentifier path = verifyNotNull((YangInstanceIdentifier)jsonCodec.parseValue(null, argument));
        final InstanceIdentifier<?> ret = bindingCodec.toBinding(path);
        checkArgument(ret != null, "%s does not have a binding representation", path);
        return ret;
    }

    // Mock wiring for JSON codec. Perhaps we should really bind to context-ref, or receive the class, or something.
    private static final class FakeLeafDefinition implements InstanceIdentifierTypeDefinition, TypeAware {
        @Override
        public Optional<String> getReference() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> getDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Status getStatus() {
            throw new UnsupportedOperationException();
        }

        @Override
        public QName getQName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> getUnits() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<? extends Object> getDefaultValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InstanceIdentifierTypeDefinition getBaseType() {
            return null;
        }

        @Override
        public boolean requireInstance() {
            return false;
        }

        @Override
        public TypeDefinition<? extends TypeDefinition<?>> getType() {
            return this;
        }
    }
}
