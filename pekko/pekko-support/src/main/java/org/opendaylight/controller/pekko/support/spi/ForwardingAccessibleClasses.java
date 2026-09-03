/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.pekko.support.spi;

import com.google.common.collect.ForwardingObject;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Abstract base {@link PekkoAccessibleClasses} forwarding to a {@link #delegate()}.
 *
 * @since 14.0.0
 */
@NonNullByDefault
public abstract non-sealed class ForwardingAccessibleClasses extends ForwardingObject
        implements PekkoAccessibleClasses {
    @Override
    protected abstract PekkoAccessibleClasses delegate();

    @Override
    public ClassLoader asClassLoader() {
        return delegate().asClassLoader();
    }
}
