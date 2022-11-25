/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.dom.spi.ContentRoutedRpcContext;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.stmt.RpcEffectiveStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for dealing with various aspects of RPCs and actions.
 */
final class RpcUtil {
    private static final Logger LOG = LoggerFactory.getLogger(RpcUtil.class);

    private RpcUtil() {
        // Hidden on purpose
    }

    static List<QName> decomposeRpcService(final Class<RpcService> service,
            final EffectiveModelContext schemaContext, final Predicate<ContentRoutedRpcContext> filter) {
        final var moduleName = BindingReflections.getQNameModule(service);
        final var module = schemaContext.findModuleStatement(moduleName)
            .orElseThrow(() -> new IllegalArgumentException("Module not found in SchemaContext: " + moduleName
                + "; service: " + service));
        LOG.debug("Resolved service {} to module {}", service, module);

        return module.streamEffectiveSubstatements(RpcEffectiveStatement.class)
            .filter(rpc -> filter.test(ContentRoutedRpcContext.forRpc(rpc)))
            .map(RpcEffectiveStatement::argument)
            .collect(Collectors.toList());
    }
}
