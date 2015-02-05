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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

abstract class AbstractDOMRpcRoutingTableEntry {
    private final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls;
    private final SchemaPath schemaPath;

    protected AbstractDOMRpcRoutingTableEntry(final SchemaPath schemaPath, final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls) {
        this.schemaPath = Preconditions.checkNotNull(schemaPath);
        this.impls = Preconditions.checkNotNull(impls);
    }

    protected final SchemaPath getSchemaPath() {
        return schemaPath;
    }

    protected final List<DOMRpcImplementation> getImplementations(final YangInstanceIdentifier context) {
        return impls.get(context);
    }

    public boolean containsContext(final YangInstanceIdentifier contextReference) {
        return impls.containsKey(contextReference);
    }

    final Set<YangInstanceIdentifier> registeredIdentifiers() {
        return impls.keySet();
    }

    final AbstractDOMRpcRoutingTableEntry add(final DOMRpcImplementation implementation, final List<YangInstanceIdentifier> newRpcs) {
        final Builder<YangInstanceIdentifier, List<DOMRpcImplementation>> vb = ImmutableMap.builder();
        for (Entry<YangInstanceIdentifier, List<DOMRpcImplementation>> ve : impls.entrySet()) {
            if (newRpcs.remove(ve.getKey())) {
                final ArrayList<DOMRpcImplementation> i = new ArrayList<>(ve.getValue().size() + 1);
                i.addAll(ve.getValue());
                i.add(implementation);
                vb.put(ve.getKey(), i);
            } else {
                vb.put(ve);
            }
        }

        return newInstance(vb.build());
    }

    final AbstractDOMRpcRoutingTableEntry remove(final DOMRpcImplementation implementation, final List<YangInstanceIdentifier> removed) {
        final Builder<YangInstanceIdentifier, List<DOMRpcImplementation>> vb = ImmutableMap.builder();
        for (Entry<YangInstanceIdentifier, List<DOMRpcImplementation>> ve : impls.entrySet()) {
            if (removed.remove(ve.getKey())) {
                final ArrayList<DOMRpcImplementation> i = new ArrayList<>(ve.getValue());
                i.remove(implementation);
                // We could trimToSize(), but that may perform another copy just to get rid
                // of a single element. That is probably not worth the trouble.
                if (!i.isEmpty()) {
                    vb.put(ve.getKey(), i);
                }
            } else {
                vb.put(ve);
            }
        }

        final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> v = vb.build();
        return v.isEmpty() ? null : newInstance(v);
    }

    protected abstract CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final NormalizedNode<?, ?> input);
    protected abstract AbstractDOMRpcRoutingTableEntry newInstance(final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> impls);
}
