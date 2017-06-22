/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

/**
 * A tagging interface that specifies a message whose serialized size can be large and thus should be sliced into
 * smaller chunks when transporting over the wire.
 *
 * @author Thomas Pantelis
 */
public interface SliceableMessage {
}
