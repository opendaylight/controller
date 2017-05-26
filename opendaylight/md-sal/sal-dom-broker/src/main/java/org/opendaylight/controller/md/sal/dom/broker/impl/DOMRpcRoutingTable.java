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
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.broker.spi.rpc.RpcRoutingStrategy;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

final class DOMRpcRoutingTable {

    static final DOMRpcRoutingTable EMPTY = new DOMRpcRoutingTable(ImmutableMap.of(), null);

    private final Map<SchemaPath, AbstractDOMRpcRoutingTableEntry> rpcs;
    private final SchemaContext schemaContext;

    private DOMRpcRoutingTable(final Map<SchemaPath, AbstractDOMRpcRoutingTableEntry> rpcs, final SchemaContext schemaContext) {
        this.rpcs = Preconditions.checkNotNull(rpcs);
        this.schemaContext = schemaContext;
    }

    static ListMultimap<SchemaPath, YangInstanceIdentifier> decomposeIdentifiers(final Set<DOMRpcIdentifier> rpcs) {
        final ListMultimap<SchemaPath, YangInstanceIdentifier> ret = LinkedListMultimap.create();
        for (DOMRpcIdentifier i : rpcs) {
            ret.put(i.getType(), i.getContextReference());
        }
        return ret;
    }

    DOMRpcRoutingTable add(final DOMRpcImplementation implementation, final Set<DOMRpcIdentifier> rpcs) {
        if (rpcs.isEmpty()) {
            return this;
        }

        // First decompose the identifiers to a multimap
        final ListMultimap<SchemaPath, YangInstanceIdentifier> toAdd = decomposeIdentifiers(rpcs);

        // Now iterate over existing entries, modifying them as appropriate...
        final Builder<SchemaPath, AbstractDOMRpcRoutingTableEntry> mb = ImmutableMap.builder();
        for (Entry<SchemaPath, AbstractDOMRpcRoutingTableEntry> re : this.rpcs.entrySet()) {
            List<YangInstanceIdentifier> newRpcs = new ArrayList<>(toAdd.removeAll(re.getKey()));
            if (!newRpcs.isEmpty()) {
                final AbstractDOMRpcRoutingTableEntry ne = re.getValue().add(implementation, newRpcs);
                mb.put(re.getKey(), ne);
            } else {
                mb.put(re);
            }
        }

        // Finally add whatever is left in the decomposed multimap
        for (Entry<SchemaPath, Collection<YangInstanceIdentifier>> e : toAdd.asMap().entrySet()) {
            final Builder<YangInstanceIdentifier, List<DOMRpcImplementation>> vb = ImmutableMap.builder();
            final List<DOMRpcImplementation> v = Collections.singletonList(implementation);
            for (YangInstanceIdentifier i : e.getValue()) {
                vb.put(i, v);
            }

            mb.put(e.getKey(), createRpcEntry(schemaContext, e.getKey(), vb.build()));
        }

        return new DOMRpcRoutingTable(mb.build(), schemaContext);
    }

    DOMRpcRoutingTable remove(final DOMRpcImplementation implementation, final Set<DOMRpcIdentifier> rpcs) {
        if (rpcs.isEmpty()) {
            return this;
        }

        // First decompose the identifiers to a multimap
        final ListMultimap<SchemaPath, YangInstanceIdentifier> toRemove = decomposeIdentifiers(rpcs);

        // Now iterate over existing entries, modifying them as appropriate...
        final Builder<SchemaPath, AbstractDOMRpcRoutingTableEntry> b = ImmutableMap.builder();
        for (Entry<SchemaPath, AbstractDOMRpcRoutingTableEntry> e : this.rpcs.entrySet()) {
            final List<YangInstanceIdentifier> removed = new ArrayList<>(toRemove.removeAll(e.getKey()));
            if (!removed.isEmpty()) {
                final AbstractDOMRpcRoutingTableEntry ne = e.getValue().remove(implementation, removed);
                if (ne != null) {
                    b.put(e.getKey(), ne);
                }
            } else {
                b.put(e);
            }
        }

        // All done, whatever is in toRemove, was not there in the first place
        return new DOMRpcRoutingTable(b.build(), schemaContext);
    }

    boolean contains(final DOMRpcIdentifier input) {
        final AbstractDOMRpcRoutingTableEntry contexts = rpcs.get(input.getType());
        return contexts != null && contexts.containsContext(input.getContextReference());
    }

    Map<SchemaPath, Set<YangInstanceIdentifier>> getRpcs(final DOMRpcAvailabilityListener l) {
        final Map<SchemaPath, Set<YangInstanceIdentifier>> ret = new HashMap<>(rpcs.size());
        for (Entry<SchemaPath, AbstractDOMRpcRoutingTableEntry> e : rpcs.entrySet()) {
            final Set<YangInstanceIdentifier> ids = e.getValue().registeredIdentifiers(l);
            if (!ids.isEmpty()) {
                ret.put(e.getKey(), ids);
            }
        }

        return ret;
    }

    private static RpcDefinition findRpcDefinition(final SchemaContext context, final SchemaPath schemaPath) {
        if (context != null) {
            final QName qname = schemaPath.getPathFromRoot().iterator().next();
            final Module module = context.findModuleByNamespaceAndRevision(qname.getNamespace(), qname.getRevision());
            if (module != null && module.getRpcs() != null) {
                for (RpcDefinition rpc : module.getRpcs()) {
                    if (qname.equals(rpc.getQName())) {
                        return rpc;
                    }
                }
            }
        }

        return null;
    }

    private static AbstractDOMRpcRoutingTableEntry createRpcEntry(final SchemaContext context, final SchemaPath key,
            final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> implementations) {
        final RpcDefinition rpcDef = findRpcDefinition(context, key);
        if (rpcDef == null) {
            return new UnknownDOMRpcRoutingTableEntry(key, implementations);
        }

        final RpcRoutingStrategy strategy = RpcRoutingStrategy.from(rpcDef);
        if (strategy.isContextBasedRouted()) {
            return new RoutedDOMRpcRoutingTableEntry(rpcDef, YangInstanceIdentifier.of(strategy.getLeaf()),
                implementations);

        }

        return new GlobalDOMRpcRoutingTableEntry(rpcDef, implementations);
    }

    CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type, final NormalizedNode<?, ?> input) {
        final AbstractDOMRpcRoutingTableEntry entry = rpcs.get(type);
        if (entry == null) {
            return Futures.<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(
                new DOMRpcImplementationNotAvailableException("No implementation of RPC %s available", type));
        }

        return entry.invokeRpc(input);
    }

    DOMRpcRoutingTable setSchemaContext(final SchemaContext context) {
        final Builder<SchemaPath, AbstractDOMRpcRoutingTableEntry> b = ImmutableMap.builder();

        for (Entry<SchemaPath, AbstractDOMRpcRoutingTableEntry> e : rpcs.entrySet()) {
            b.put(e.getKey(), createRpcEntry(context, e.getKey(), e.getValue().getImplementations()));
        }

        return new DOMRpcRoutingTable(b.build(), context);
    }
}
