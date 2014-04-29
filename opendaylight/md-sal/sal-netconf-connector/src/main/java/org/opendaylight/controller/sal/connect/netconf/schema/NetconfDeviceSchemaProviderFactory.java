/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.schema;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;

import org.opendaylight.controller.sal.connect.api.SchemaContextProviderFactory;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.util.repo.SchemaSourceProvider;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.opendaylight.yangtools.yang.parser.impl.util.YangSourceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public final class NetconfDeviceSchemaProviderFactory implements SchemaContextProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(NetconfDeviceSchemaProviderFactory.class);

    private final RemoteDeviceId id;

    public NetconfDeviceSchemaProviderFactory(final RemoteDeviceId id) {
        this.id = id;
    }

    @Override
    public SchemaContextProvider createContextProvider(final Collection<QName> capabilities, final SchemaSourceProvider<InputStream> sourceProvider) {

        final YangSourceContext sourceContext = YangSourceContext.createFrom(capabilities, sourceProvider);

        if (sourceContext.getMissingSources().isEmpty() == false) {
            logger.warn("{}: Sources for following models are missing {}", id, sourceContext.getMissingSources());
        }

        logger.debug("{}: Trying to create schema context from {}", id, sourceContext.getValidSources());
        final List<InputStream> modelsToParse = YangSourceContext.getValidInputStreams(sourceContext);

        Preconditions.checkState(sourceContext.getValidSources().isEmpty() == false,
                "%s: Unable to create schema context, no sources provided by device", id);
        try {
            final SchemaContext schemaContext = tryToParseContext(modelsToParse);
            logger.debug("{}: Schema context successfully created.", id);
            return new NetconfSchemaContextProvider(schemaContext);
        } catch (final RuntimeException e) {
            logger.error("{}: Unable to create schema context, unexpected error", id, e);
            throw new IllegalStateException(id + ": Unable to create schema context", e);
        }
    }

    private static SchemaContext tryToParseContext(final List<InputStream> modelsToParse) {
        final YangParserImpl parser = new YangParserImpl();
        final Set<Module> models = parser.parseYangModelsFromStreams(modelsToParse);
        return parser.resolveSchemaContext(models);
    }

    private static final class NetconfSchemaContextProvider implements SchemaContextProvider {
        private final SchemaContext schemaContext;

        public NetconfSchemaContextProvider(final SchemaContext schemaContext) {
            this.schemaContext = schemaContext;
        }

        @Override
        public SchemaContext getSchemaContext() {
            return schemaContext;
        }
    }
}
