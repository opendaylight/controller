/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */


package org.opendaylight.controller.stream;


import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.io.IOException;
import java.net.URISyntaxException;


public interface NormalizedNodeStreamReader extends AutoCloseable {

    NormalizedNode<?, ?> readNormalizedNode() throws IOException, ClassNotFoundException, URISyntaxException;
}
