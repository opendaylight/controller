/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import java.util.Collections;
import java.util.Map;

/**
 * ModuleShardConfigProvider implementation that returns an empty map.
 *
 * @author Thomas Pantelis
 */
public class EmptyModuleShardConfigProvider implements ModuleShardConfigProvider {

    @Override
    public Map<String, ModuleConfig.Builder> retrieveModuleConfigs(Configuration configuration) {
        return Collections.emptyMap();
    }
}
