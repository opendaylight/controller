/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.collect.ForwardingObject;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;

/**
 * Utility {@link DOMTransactionChain} implementation which forwards all interface
 * method invocation to a delegate instance.
 */
public abstract class ForwardingDOMTransactionChain extends ForwardingObject implements DOMTransactionChain {
    @Override
    protected abstract @Nonnull DOMTransactionChain delegate();

    @Override
    public void close() {
        delegate().close();
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return delegate().newReadOnlyTransaction();
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return delegate().newReadWriteTransaction();
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return delegate().newWriteOnlyTransaction();
    }
}
