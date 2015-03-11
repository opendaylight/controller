/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.spi;


import com.google.common.base.Preconditions;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import java.util.Set;
import org.opendaylight.yangtools.concepts.Builder;

public abstract class AdapterBuilder<T,D> implements Builder<T> {

    private final ClassToInstanceMap<D> delegates = MutableClassToInstanceMap.create();

    public abstract Set<? extends Class<? extends D>> getRequiredDelegates();

    protected abstract T createInstance(ClassToInstanceMap<D> delegates);

    private void checkAllRequiredServices() {
        for(final Class<? extends D> type : getRequiredDelegates()) {
            Preconditions.checkState(delegates.get(type) != null, "Requires service %s is not defined.",type);
        }
    }

    public final <V extends D>void addDelegate(final Class<V> type,final D impl) {
        delegates.put(type,impl);
    }

    @Override
    public final  T build() {
        checkAllRequiredServices();
        return createInstance(ImmutableClassToInstanceMap.<D,D>copyOf(delegates));
    }

}
