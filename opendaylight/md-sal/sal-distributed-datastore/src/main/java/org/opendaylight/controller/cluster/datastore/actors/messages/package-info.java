/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * Package containing internal messages exchanged between backend actors. These messages are not meant for public
 * consumption and are not {@link java.io.Serializable} on purpose, as they are exchanged strictly inside the local
 * actor system.
 */
package org.opendaylight.controller.cluster.datastore.actors.messages;