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
import org.opendaylight.controller.md.sal.binding.api.ActionService;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMAdapterBuilder.Factory;
import org.opendaylight.controller.md.sal.dom.api.DOMActionService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.dom.adapter.ActionServiceAdapter;
import org.opendaylight.yangtools.yang.binding.Action;
import org.opendaylight.yangtools.yang.binding.DataObject;

final class ControllerActionServiceAdapter implements ActionService {
    private static final class Builder extends BindingDOMAdapterBuilder<ActionService> {
        @Override
        public Set<? extends Class<? extends DOMService>> getRequiredDelegates() {
            return ImmutableSet.of(DOMActionService.class);
        }

        @Override
        protected ActionService createInstance(final BindingToNormalizedNodeCodec codec,
                final ClassToInstanceMap<DOMService> delegates) {
            final DOMActionService domAction = delegates.getInstance(DOMActionService.class);
            return new ControllerActionServiceAdapter(requireNonNull(codec), domAction);
        }
    }

    static final Factory<ActionService> BUILDER_FACTORY = Builder::new;
    private final ActionServiceAdapter delegate;

    ControllerActionServiceAdapter(final BindingToNormalizedNodeCodec codec, final DOMActionService domService) {
        this.delegate = ActionServiceAdapter.create(codec, domService);
    }

    @Override
    public <O extends @NonNull DataObject, T extends @NonNull Action<?, ?, ?>> T getActionHandle(
            final Class<T> actionInterface, final Set<DataTreeIdentifier<O>> validNodes) {
        return delegate.getActionHandle(actionInterface, validNodes);
    }
}
