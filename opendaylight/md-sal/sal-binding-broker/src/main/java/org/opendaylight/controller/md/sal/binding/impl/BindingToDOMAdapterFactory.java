/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.spi.AdapterFactory;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;

@Beta
@NonNullByDefault
public final class BindingToDOMAdapterFactory implements AdapterFactory {
    private final BindingToNormalizedNodeCodec codec;

    public BindingToDOMAdapterFactory(final BindingToNormalizedNodeCodec codec) {
        this.codec = requireNonNull(codec);
    }

    @Override
    public DataBroker createDataBroker(final DOMDataBroker domBroker) {
        return new BindingDOMDataBrokerAdapter(requireNonNull(domBroker), codec);
    }
}
