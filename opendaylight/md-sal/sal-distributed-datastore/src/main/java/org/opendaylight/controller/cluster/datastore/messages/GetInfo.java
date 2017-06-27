/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

/**
 * Local message sent to an actor to retrieve internal information for reporting.
 *
 * @author Thomas Pantelis
 */
public final class GetInfo {
    public static final GetInfo INSTANCE = new GetInfo();

    private GetInfo() {
    }
}
