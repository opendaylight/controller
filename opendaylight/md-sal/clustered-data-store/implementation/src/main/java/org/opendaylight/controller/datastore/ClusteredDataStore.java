
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.datastore;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface ClusteredDataStore extends DataReader<InstanceIdentifier<? extends Object>, Object>, DataCommitHandler<InstanceIdentifier<? extends Object>,Object> {
}
