/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.schema.provider;

import com.google.common.annotations.Beta;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.schema.provider.impl.YangTextSchemaSourceSerializationProxy;
import org.opendaylight.yangtools.yang.model.api.source.SourceIdentifier;

/**
 * A remote yang text source provider provides serializable yang text sources.
 */
@Beta
@NonNullByDefault
public interface RemoteYangTextSourceProvider {

    CompletionStage<Set<SourceIdentifier>> getProvidedSources();

    CompletionStage<YangTextSchemaSourceSerializationProxy> getYangTextSchemaSource(SourceIdentifier identifier);
}
