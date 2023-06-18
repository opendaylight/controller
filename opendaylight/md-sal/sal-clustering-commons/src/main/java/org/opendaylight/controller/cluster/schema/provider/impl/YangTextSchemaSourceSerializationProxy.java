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
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

/**
 * {@link org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource} serialization proxy.
 */
@Beta
public class YangTextSchemaSourceSerializationProxy implements Serializable {
    private static final long serialVersionUID = -6361268518176019477L;

    private final String schemaSource;
    private final Revision revision;
    private final String name;

    public YangTextSchemaSourceSerializationProxy(final YangTextSchemaSource source) throws IOException {
        final var id = source.getIdentifier();
        revision = id.revision();
        name = id.name().getLocalName();
        schemaSource = source.read();
    }

    public YangTextSchemaSource getRepresentation() {
        return YangTextSchemaSource.delegateForCharSource(new SourceIdentifier(Unqualified.of(name), revision),
            CharSource.wrap(schemaSource));
    }
}
