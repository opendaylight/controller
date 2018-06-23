/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import org.opendaylight.mdsal.dom.spi.RpcRoutingStrategy;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for dealing with various aspects of RPCs and actions.
 *
 * @author Robert Varga
 */
final class RpcUtil {
    private static final Logger LOG = LoggerFactory.getLogger(RpcUtil.class);

    private RpcUtil() {
        throw new UnsupportedOperationException();
    }

    static Collection<SchemaPath> decomposeRpcService(final Class<RpcService> service,
            final SchemaContext schemaContext, final Predicate<RpcRoutingStrategy> filter) {
        final QNameModule moduleName = BindingReflections.getQNameModule(service);
        final Module module = schemaContext.findModule(moduleName).get();
        LOG.debug("Resolved service {} to module {}", service, module);

        final Collection<RpcDefinition> rpcs = module.getRpcs();
        final Collection<SchemaPath> ret = new ArrayList<>(rpcs.size());
        for (RpcDefinition rpc : rpcs) {
            final RpcRoutingStrategy strategy = RpcRoutingStrategy.from(rpc);
            if (filter.test(strategy)) {
                ret.add(rpc.getPath());
            }
        }

        return ret;
    }
}
