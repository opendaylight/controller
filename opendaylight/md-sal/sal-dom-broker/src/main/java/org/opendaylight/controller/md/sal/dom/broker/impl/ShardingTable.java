/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import java.util.Collections;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

// FIXME: this is a prefix tree
final class ShardingTable {
    private final ShardingTableEntry root = new ShardingTableEntry(Collections.<PathArgument>emptyList());

    ShardRegistration<?> lookup(final YangInstanceIdentifier id) {
        return root.lookup(id);
    }

    void store(final YangInstanceIdentifier id, final ShardRegistration<?> reg) {
        root.store(id, reg);
    }

}
