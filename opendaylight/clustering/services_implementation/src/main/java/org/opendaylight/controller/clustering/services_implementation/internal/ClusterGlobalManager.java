
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.services_implementation.internal;

import java.util.Map;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterGlobalManager
    extends ClusterManagerCommon
    implements IClusterGlobalServices {
    protected static final Logger logger = LoggerFactory.getLogger(ClusterGlobalManager.class);

    @Override
    void setCacheUpdateAware(Map props, ICacheUpdateAware s) {
        logger.trace("setCacheUpdateAware");
        if (props.get("containerName") != null) {
            // If we got a reference with the containerName property
            // that is not what we are looking for, so filter it out.
            return;
        }
        super.setCacheUpdateAware(props, s);
    }

    @Override
    void unsetCacheUpdateAware(Map props, ICacheUpdateAware s) {
        logger.trace("unsetCacheUpdateAware");
        if (props.get("containerName") != null) {
            // If we got a reference with the containerName property
            // that is not what we are looking for, so filter it out.
            return;
        }
        super.unsetCacheUpdateAware(props, s);
    }
}
