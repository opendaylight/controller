/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Interface for a class that provides module and shard configuration information.
 *
 * @author Thomas Pantelis
 */
public interface ModuleShardConfigProvider {
    /**
     * Returns a Map of ModuleConfig Builder instances keyed by module name.
     */
    @Nonnull Map<String, ModuleConfig.Builder> retrieveModuleConfigs(@Nonnull Configuration configuration);
}
