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

public interface DataChange<P/* extends Path<P> */, D> {

    /**
     * Returns a map of paths and newly created objects
     * 
     * @return map of paths and newly created objects
     */
    Map<P, D> getCreatedOperationalData();

    /**
     * Returns a map of paths and newly created objects
     * 
     * @return map of paths and newly created objects
     */
    Map<P, D> getCreatedConfigurationData();

    /**
     * Returns a map of paths and respective updated objects after update.
     * 
     * Original state of the object is in
     * {@link #getOriginalOperationalData()}
     * 
     * @return map of paths and newly created objects
     */
    Map<P, D> getUpdatedOperationalData();

    /**
     * Returns a map of paths and respective updated objects after update.
     * 
     * Original state of the object is in
     * {@link #getOriginalConfigurationData()}
     * 
     * @return map of paths and newly created objects
     */
    Map<P, D> getUpdatedConfigurationData();



    /**
     * Returns a set of paths of removed objects.
     * 
     * Original state of the object is in
     * {@link #getOriginalConfigurationData()}
     * 
     * @return map of paths and newly created objects
     */
    Set<P> getRemovedConfigurationData();

    /**
     * Returns a set of paths of removed objects.
     * 
     * Original state of the object is in
     * {@link #getOriginalOperationalData()}
     * 
     * @return map of paths and newly created objects
     */
    Set<P> getRemovedOperationalData();

    /**
     * Return a map of paths and original state of updated and removed objectd.
     * 
     * @return map of paths and original state of updated and removed objectd.
     */
    Map<P, D> getOriginalConfigurationData();

    /**
     * Return a map of paths and original state of updated and removed objectd.
     * 
     * @return map of paths and original state of updated and removed objectd.
     */
    Map<P, D> getOriginalOperationalData();
}
