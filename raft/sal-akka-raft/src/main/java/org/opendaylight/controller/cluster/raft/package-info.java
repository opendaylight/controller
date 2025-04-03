/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
@org.osgi.annotation.bundle.Export
// Needed for Class.forName() we use to instantiate RaftPolicy. It was originally introduce to deal with serialization
// issues, so take needs to taken with testing in OSGi if this is to be removed.
@org.osgi.annotation.bundle.Header(name = org.osgi.framework.Constants.DYNAMICIMPORT_PACKAGE, value = "*")
package org.opendaylight.controller.cluster.raft;
