/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.schema.provider.impl;

import com.google.common.annotations.Beta;
import com.google.common.io.CharSource;
import java.io.IOException;
import java.io.Serializable;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.UnresolvedQName.Unqualified;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.api.source.YangTextSource;
import org.opendaylight.yangtools.yang.model.spi.source.DelegatedYangTextSource;

/**
 * {@link YangTextSource} serialization proxy.
 */
@Beta
public class YangTextSchemaSourceSerializationProxy implements Serializable {
    private static final long serialVersionUID = -6361268518176019477L;

    private final String schemaSource;
    private final Revision revision;
    private final String name;

    public YangTextSchemaSourceSerializationProxy(final YangTextSource source) throws IOException {
        final var sourceId = source.sourceId();
        revision = sourceId.revision();
        name = sourceId.name().getLocalName();
        schemaSource = source.read();
    }

    public YangTextSource getRepresentation() {
        return new DelegatedYangTextSource(new SourceIdentifier(Unqualified.of(name), revision),
            CharSource.wrap(schemaSource));
    }
}
