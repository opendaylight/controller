/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

final class DOMRpcRoutingTable {
    static final DOMRpcRoutingTable EMPTY = new DOMRpcRoutingTable(Collections.<DOMRpcIdentifier, List<DOMRpcImplementation>>emptyMap());
    /*
     * We are not using a Multimap, but rather an implicit Map so we have control over
     * value reuse, as most of the time most collections will remain unmodified.
     * We never use leak a mutable view of the values, so using ArrayLists is safe as
     * long as we take care to copy them when mutating.
     */
    private final Map<DOMRpcIdentifier, List<DOMRpcImplementation>> rpcs;

    private DOMRpcRoutingTable(final Map<DOMRpcIdentifier, List<DOMRpcImplementation>> rpcs) {
        this.rpcs = Preconditions.checkNotNull(rpcs);
    }

    DOMRpcRoutingTable add(final DOMRpcImplementation implementation, final Collection<DOMRpcIdentifier> rpcs) {
        if (rpcs.isEmpty()) {
            return this;
        }

        final Builder<DOMRpcIdentifier, List<DOMRpcImplementation>> b = ImmutableMap.builder();
        for (Entry<DOMRpcIdentifier, List<DOMRpcImplementation>> e : this.rpcs.entrySet()) {
            if (rpcs.contains(e.getKey())) {
                final ArrayList<DOMRpcImplementation> i = new ArrayList<>(e.getValue().size() + 1);
                i.addAll(e.getValue());
                i.add(implementation);
                b.put(e.getKey(), i);
            } else {
                b.put(e);
            }
        }

        return new DOMRpcRoutingTable(b.build());
    }

    DOMRpcRoutingTable remove(final DOMRpcImplementation implementation, final Collection<DOMRpcIdentifier> rpcs) {
        if (rpcs.isEmpty()) {
            return this;
        }

        final Builder<DOMRpcIdentifier, List<DOMRpcImplementation>> b = ImmutableMap.builder();
        // We cannot use filter, as it is possible to use multiple registrations
        for (Entry<DOMRpcIdentifier, List<DOMRpcImplementation>> e : this.rpcs.entrySet()) {
            if (rpcs.contains(e.getKey())) {
                final ArrayList<DOMRpcImplementation> i = new ArrayList<>(e.getValue());
                i.remove(implementation);
                // We could trimToSize(), but that may perform another copy just to get rid
                // of a single element. That is probably not worth the trouble.
                b.put(e.getKey(), i);
            } else {
                b.put(e);
            }
        }

        return new DOMRpcRoutingTable(b.build());
    }

    boolean contains(final DOMRpcIdentifier input) {
        return rpcs.containsKey(input);
    }

    Set<DOMRpcIdentifier> getRpcs() {
        return rpcs.keySet();
    }

    CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type, final NormalizedNode<?, ?> input) {
        // FIXME: deal with routed RPCs
        final DOMRpcIdentifier rpcId = DOMRpcIdentifier.create(type);
        final List<DOMRpcImplementation> impls = rpcs.get(rpcId);
        if (impls.isEmpty()) {
            return Futures.<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(new DOMRpcImplementationNotAvailableException("No implementation of RPC %s available", rpcId));
        }

        return impls.get(0).invokeRpc(rpcId, input);
    }
}
