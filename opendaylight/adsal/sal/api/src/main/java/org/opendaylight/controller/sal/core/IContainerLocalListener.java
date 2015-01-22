/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file IContainerLocalListener.java
 *
 * @brief Set of methods needed to listen to changes in the Container
 *        configuration for listeners on the local cluster node only
 */
package org.opendaylight.controller.sal.core;


/**
 * The interface describes methods used to publish the changes to a given
 * Container configuration to listeners on the local cluster node only.
 */
@Deprecated
public interface IContainerLocalListener extends IContainerListener {

}
