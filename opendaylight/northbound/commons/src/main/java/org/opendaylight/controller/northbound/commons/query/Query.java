/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import java.util.List;

/**
 *
 *
 */
public interface Query<T> {

    /**
     * Filter object based on search condition and return the list of filtered
     * objects
     * @param resourceList
     * @return
     */
    public List<T> filter(List<T> resourceList);
}
