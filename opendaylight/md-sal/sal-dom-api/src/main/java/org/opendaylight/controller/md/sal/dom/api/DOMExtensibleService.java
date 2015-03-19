/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.annotations.Beta;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Marker interface for services which can support {@link DOMServiceExtension}.
 * Aside for marking these, they also provide runtime query to detect whether
 * a particular trait is in fact available.
 *
 * <T> Base {@link DOMService}
 * <E> Extension type
 */
@Beta
public interface DOMExtensibleService<T extends DOMExtensibleService<T, E>, E extends DOMServiceExtension<T, E>> extends DOMService {
    /**
     * Return a map of currently-supported extensions, along with accessor services
     * which provide access to the specific functionality bound to this service.
     *
     * @return A map of supported functionality.
     */
    @Nonnull Map<Class<? extends E>, E> getSupportedExtensions();
}
