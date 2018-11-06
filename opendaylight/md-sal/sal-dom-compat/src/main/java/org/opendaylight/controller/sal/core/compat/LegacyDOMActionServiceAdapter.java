/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMActionResult;
import org.opendaylight.mdsal.dom.api.DOMActionServiceExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Deprecated
public class LegacyDOMActionServiceAdapter extends ForwardingObject implements DOMActionService {
    private final org.opendaylight.mdsal.dom.api.DOMActionService delegate;

    public LegacyDOMActionServiceAdapter(final org.opendaylight.mdsal.dom.api.DOMActionService delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected org.opendaylight.mdsal.dom.api.DOMActionService delegate() {
        return delegate;
    }

    @Override
    public FluentFuture<? extends DOMActionResult> invokeAction(final SchemaPath type, final DOMDataTreeIdentifier path,
            final ContainerNode input) {
        return delegate.invokeAction(type, path, input);
    }

    @Override
    public ClassToInstanceMap<DOMActionServiceExtension> getExtensions() {
        return delegate.getExtensions();
    }
}
