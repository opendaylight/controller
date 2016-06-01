/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * A combination of both {@link Identifier} and {@link WritableObject}.
 *
 * @author Robert Varga
 */
// FIXME: this should reside in yangtools/concepts
@Beta
public interface WritableIdentifier extends Identifier, WritableObject {

}
