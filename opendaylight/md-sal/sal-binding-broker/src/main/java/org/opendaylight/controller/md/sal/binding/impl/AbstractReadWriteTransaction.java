/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractReadWriteTransaction extends AbstractWriteTransaction<DOMDataReadWriteTransaction> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractReadWriteTransaction.class);

    public AbstractReadWriteTransaction(final DOMDataReadWriteTransaction delegate, final BindingToNormalizedNodeCodec codec) {
        super(delegate, codec);
    }

}
