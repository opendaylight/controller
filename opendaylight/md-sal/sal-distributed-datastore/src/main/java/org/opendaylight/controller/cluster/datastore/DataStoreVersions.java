/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

/**
 * Defines version numbers.
 *
 * @author Thomas Pantelis
 */
public interface DataStoreVersions {
    short BASE_HELIUM_VERSION = 0;
    short HELIUM_1_VERSION = 1;
    short HELIUM_2_VERSION = 2;
    short CURRENT_VERSION = HELIUM_2_VERSION;
}
