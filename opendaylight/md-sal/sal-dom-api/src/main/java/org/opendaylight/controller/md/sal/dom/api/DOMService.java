/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.sal.core.api.BrokerService;

/**
 * Marker interface for services which can be obtained from a {@link DOMMountPoint}
 * instance. No further semantics are implied.
 */
public interface DOMService extends BrokerService {

}
