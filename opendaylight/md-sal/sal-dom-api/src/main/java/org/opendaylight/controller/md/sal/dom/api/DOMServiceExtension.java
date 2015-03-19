/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.annotations.Beta;

/**
 * Marker interface for services which expose additional functionality on top
 * of some base {@link DOMService}.
 */
@Beta
public interface DOMServiceExtension<T extends DOMExtensibleService<T, E>, E extends DOMServiceExtension<T, E>> {

}
