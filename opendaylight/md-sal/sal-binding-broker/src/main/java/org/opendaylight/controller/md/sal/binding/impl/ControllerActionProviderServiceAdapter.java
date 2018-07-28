/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.ActionProviderService;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.dom.api.DOMActionProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.dom.adapter.ActionProviderServiceAdapter;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.Action;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class ControllerActionProviderServiceAdapter implements ActionProviderService {
    private static final class Builder extends BindingDOMAdapterBuilder<ActionProviderService> {
        @Override
        public Set<? extends Class<? extends DOMService>> getRequiredDelegates() {
            return ImmutableSet.of(DOMActionProviderService.class);
        }

        @Override
        protected ActionProviderService createInstance(BindingToNormalizedNodeCodec codec,
                ClassToInstanceMap<DOMService> delegates) {
            final DOMActionProviderService domAction = delegates.getInstance(DOMActionProviderService.class);
            return new ControllerActionProviderServiceAdapter(requireNonNull(codec), domAction);
        }
    }

    static final Factory<ActionProviderService> BUILDER_FACTORY = Builder::new;

    private final ActionProviderServiceAdapter delegate;

    ControllerActionProviderServiceAdapter(BindingToNormalizedNodeCodec codec, DOMActionProviderService domService) {
        this.delegate = ActionProviderServiceAdapter.create(codec, domService);
    }

    @Override
    public <O extends @NonNull DataObject, P extends @NonNull InstanceIdentifier<O>, T extends @NonNull Action<P, ?, ?>,
            S extends T> ObjectRegistration<S> registerImplementation(Class<T> actionInterface, S implementation,
                    LogicalDatastoreType datastore, Set<DataTreeIdentifier<O>> validNodes) {
        return delegate.registerImplementation(actionInterface, implementation, datastore, validNodes);
    }
}
