/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkState;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

final class DOMRpcRoutingTable {
    static final DOMRpcRoutingTable EMPTY = new DOMRpcRoutingTable();
    private static final Function<Map<YangInstanceIdentifier, ?>, Set<YangInstanceIdentifier>> EXTRACT_IDENTIFIERS =
            new Function<Map<YangInstanceIdentifier, ?>, Set<YangInstanceIdentifier>>() {
                @Override
                public Set<YangInstanceIdentifier> apply(final Map<YangInstanceIdentifier, ?> input) {
                    return input.keySet();
                }
    };
    private final Map<SchemaPath, Map<YangInstanceIdentifier, List<DOMRpcImplementation>>> rpcs;
    private final SchemaContext schemaContext;

    private DOMRpcRoutingTable() {
        rpcs = Collections.emptyMap();
        schemaContext = null;
    }

    private DOMRpcRoutingTable(final Map<SchemaPath, Map<YangInstanceIdentifier, List<DOMRpcImplementation>>> rpcs, final SchemaContext schemaContext) {
        this.rpcs = Preconditions.checkNotNull(rpcs);
        this.schemaContext = schemaContext;
    }

    private static ListMultimap<SchemaPath, YangInstanceIdentifier> decomposeIdentifiers(final Set<DOMRpcIdentifier> rpcs) {
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
        final Builder<SchemaPath, Map<YangInstanceIdentifier, List<DOMRpcImplementation>>> mb = ImmutableMap.builder();
        for (Entry<SchemaPath, Map<YangInstanceIdentifier, List<DOMRpcImplementation>>> re : this.rpcs.entrySet()) {
            List<YangInstanceIdentifier> newRpcs = toAdd.removeAll(re.getKey());
            if (!newRpcs.isEmpty()) {
                final Builder<YangInstanceIdentifier, List<DOMRpcImplementation>> vb = ImmutableMap.builder();
                for (Entry<YangInstanceIdentifier, List<DOMRpcImplementation>> ve : re.getValue().entrySet()) {
                    if (newRpcs.remove(ve.getKey())) {
                        final ArrayList<DOMRpcImplementation> i = new ArrayList<>(ve.getValue().size() + 1);
                        i.addAll(ve.getValue());
                        i.add(implementation);
                        vb.put(ve.getKey(), i);
                    } else {
                        vb.put(ve);
                    }
                }
                mb.put(re.getKey(), vb.build());
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

            mb.put(e.getKey(), vb.build());
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
        final Builder<SchemaPath, Map<YangInstanceIdentifier, List<DOMRpcImplementation>>> mb = ImmutableMap.builder();
        for (Entry<SchemaPath, Map<YangInstanceIdentifier, List<DOMRpcImplementation>>> re : this.rpcs.entrySet()) {
            final List<YangInstanceIdentifier> removed = toRemove.removeAll(re.getKey());
            if (!removed.isEmpty()) {
                final Builder<YangInstanceIdentifier, List<DOMRpcImplementation>> vb = ImmutableMap.builder();
                for (Entry<YangInstanceIdentifier, List<DOMRpcImplementation>> ve : re.getValue().entrySet()) {
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
                if (!v.isEmpty()) {
                    mb.put(re.getKey(), v);
                }
            } else {
                mb.put(re);
            }
        }

        // All done, whatever is in toRemove, was not there in the first place
        return new DOMRpcRoutingTable(mb.build(), schemaContext);
    }

    boolean contains(final DOMRpcIdentifier input) {
        final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> contexts = rpcs.get(input.getType());
        return contexts != null && contexts.containsKey(input.getContextReference());
    }

    Map<SchemaPath, Set<YangInstanceIdentifier>> getRpcs() {
        return Maps.transformValues(rpcs, EXTRACT_IDENTIFIERS);
    }

    private RpcDefinition findRpcDefinition(final QName rpcType) {
        checkState(schemaContext != null, "YANG Schema Context is not available.");
        Module module = schemaContext.findModuleByNamespaceAndRevision(rpcType.getNamespace(), rpcType.getRevision());
        checkState(module != null, "YANG Module is not available.");
        return findRpcDefinition(rpcType, module.getRpcs());
    }

    static private RpcDefinition findRpcDefinition(final QName rpcType, final Set<RpcDefinition> rpcs) {
        checkState(rpcs != null, "Rpc schema is not available.");
        for (RpcDefinition rpc : rpcs) {
            if (rpcType.equals(rpc.getQName())) {
                return rpc;
            }
        }
        throw new IllegalArgumentException("Supplied Rpc Type is not defined.");
    }

    CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(final SchemaPath type, final NormalizedNode<?, ?> input) {
        final Map<YangInstanceIdentifier, List<DOMRpcImplementation>> allImpls = rpcs.get(type);
        if (allImpls == null) {
            return Futures.<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(new DOMRpcImplementationNotAvailableException("No implementation of RPC %s available", type));
        }

        // FIXME: deal with routed RPCs

        final DOMRpcIdentifier rpcId = DOMRpcIdentifier.create(type);
        final List<DOMRpcImplementation> globalImpls = allImpls.get(rpcId);
        if (globalImpls == null) {
            return Futures.<DOMRpcResult, DOMRpcException>immediateFailedCheckedFuture(new DOMRpcImplementationNotAvailableException("No implementation of RPC %s available", rpcId));
        }

        return globalImpls.get(0).invokeRpc(rpcId, input);
    }
}
