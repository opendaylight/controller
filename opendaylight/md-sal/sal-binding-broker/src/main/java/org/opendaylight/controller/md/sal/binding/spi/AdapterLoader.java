/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.spi;

import com.google.common.base.Optional;
import com.google.common.cache.CacheLoader;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AdapterLoader<T, D> extends CacheLoader<Class<? extends T>, Optional<T>> {

    @Override
    public Optional<T> load(final Class<? extends T> key) {

        final AdapterBuilder<? extends T, D> builder = createBuilder(key);
        for(final Class<? extends D> reqDeleg : builder.getRequiredDelegates()) {
            final D deleg = getDelegate(reqDeleg);
            if(deleg != null) {
                builder.addDelegate(reqDeleg,deleg);
            } else {
                return Optional.absent();
            }
        }
        return  Optional.<T>of(builder.build());
    }

    protected abstract @Nullable D getDelegate(Class<? extends D> reqDeleg);

    protected abstract @Nonnull AdapterBuilder<? extends T, D> createBuilder(Class<? extends T> key) throws IllegalArgumentException;

}
