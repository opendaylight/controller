/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.concepts.Registration;

/**
 *
 *
 * @deprecated THis was intended as Registration object, normal use of {@link org.opendaylight.yangtools.concepts.ObjectRegistration}
 * is suffiecient, since {@link #getPath()} was implementation leak.
 *
 * @param <P>
 * @param <D>
 */
@Deprecated
public interface DataCommitHandlerRegistration<P extends Path<P>,D> extends Registration {

    P getPath();
}
