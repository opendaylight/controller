/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

// FIXME: After 0.6 Release of YANGTools refactor to use Path marker interface for arguments.
// import org.opendaylight.yangtools.concepts.Path;

/**
 * Reader for reading YANG subtrees based on their path.
 * 
 * Reader is requested to return object at specified path and all it's subnodes
 * known to the reader or null if node is not found in this reader.
 *
 * @param <P> Path Type
 * @param <D> Data Type
 */
public interface DataReader<P/* extends Path<P> */,D> {

    /**
     * Reads data from Operational data store located at provided path
     * 
     * @param path Path to data
     * @return
     */
    D readOperationalData(P path);

    D readConfigurationData(P path);
}
