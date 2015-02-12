/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

final class ShardingTableEntry {
    private final Map<PathArgument, ShardingTableEntry> children = Collections.emptyMap();
    private final Collection<PathArgument> prefix;

    ShardingTableEntry(final Collection<PathArgument> prefix) {
        this.prefix = Preconditions.checkNotNull(prefix);
    }

    ShardRegistration<?> lookup(final YangInstanceIdentifier id) {
        final Iterator<PathArgument> it = id.getPathArguments().iterator();

        while (it.hasNext()) {
            final PathArgument a = it.next();







        }

        // TODO Auto-generated method stub
        return null;
    }

    void store(final YangInstanceIdentifier id, final ShardRegistration<?> reg) {
        // FIXME: implement this
    }
}
