/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.data;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 *
 * @deprecated Replaced by org.opendaylight.controller.sal.core.spi.data.DOMStore.
 *
 */
@Deprecated
public interface DataStore extends //
    DataReader<YangInstanceIdentifier, CompositeNode>,
    DataCommitHandler<YangInstanceIdentifier, CompositeNode> {


    Iterable<YangInstanceIdentifier> getStoredConfigurationPaths();
    Iterable<YangInstanceIdentifier> getStoredOperationalPaths();

    boolean containsConfigurationPath(YangInstanceIdentifier path);
    boolean containsOperationalPath(YangInstanceIdentifier path);

}
