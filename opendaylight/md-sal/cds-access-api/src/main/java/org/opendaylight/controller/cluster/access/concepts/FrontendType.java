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
 * An {@link Identifier} identifying a data store frontend type, which is able to access the data store backend.
 * Frontend implementations need to define this identifier so that multiple clients existing on a member node can be
 * discerned.
 */
@Beta
public interface FrontendType extends Identifier {

}
