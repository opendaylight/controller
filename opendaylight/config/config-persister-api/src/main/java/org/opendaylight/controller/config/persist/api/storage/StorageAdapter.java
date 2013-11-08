/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.api.storage;

import org.opendaylight.controller.config.persist.api.Persister;
import org.osgi.framework.BundleContext;

/**
 * Plugins for {@link org.opendaylight.controller.config.persist.api.Persister}
 * must implement this interface.
 */
public interface StorageAdapter extends Persister {

    void setProperties(BundleContext bundleContext);

}
