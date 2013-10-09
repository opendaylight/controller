/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import java.util.Map;
import java.util.Set;

// FIXME: After 0.6 Release of YANGTools refactor to use Path marker interface for arguments.
// import org.opendaylight.yangtools.concepts.Path;


public interface DataChange<P/* extends Path<P> */,D> {

    Map<P,D> getCreatedOperationalData();

    Map<P,D> getUpdatedOperationalData();

    Set<P> getRemovedOperationalData();

    Map<P,D> getCreatedConfigurationData();

    Map<P,D> getUpdatedConfigurationData();

    Set<P> getRemovedConfigurationData();

}
