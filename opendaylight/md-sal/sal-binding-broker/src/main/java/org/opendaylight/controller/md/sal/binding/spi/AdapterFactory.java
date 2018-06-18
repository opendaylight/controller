/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.spi;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;

@Beta
@NonNullByDefault
public interface AdapterFactory {
    /**
     * Return a {@link DataBroker} implementation backed by the specified {@link DOMDataBroker}.
     *
     * @param domBroker Backing DOMDataBroker
     * @return A DataBroker instance.
     * @throws NullPointerException if {@code domBroker} is null.
     */
    DataBroker createDataBroker(DOMDataBroker domBroker);
}
