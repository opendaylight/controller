/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.factoriesresolver;

import java.util.List;

import org.opendaylight.controller.config.spi.ModuleFactory;

/**
 * {@link org.opendaylight.controller.config.manager.impl.ConfigTransactionControllerImpl}
 * receives list of factories using this interface. For testing, this could be
 * implemented as hard coded list of objects, for OSGi this would look for all
 * services in OSGi Service Registry are registered under
 * {@link org.opendaylight.controller.config.spi.ModuleFactory} name.
 */
public interface ModuleFactoriesResolver {

    List<ModuleFactory> getAllFactories();

}
