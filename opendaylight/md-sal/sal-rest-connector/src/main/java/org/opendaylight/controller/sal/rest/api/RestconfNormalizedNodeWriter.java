/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.api;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface RestconfNormalizedNodeWriter extends Flushable, Closeable {

    RestconfNormalizedNodeWriter write(final NormalizedNode<?, ?> node) throws IOException;
}
