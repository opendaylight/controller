/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import javax.management.DynamicMBean;

/**
 * Each {@link org.opendaylight.controller.config.spi.Module} in JMX registry
 * will be wrapped in this class.
 */
public interface DynamicMBeanModuleWrapper extends DynamicMBean {

}
