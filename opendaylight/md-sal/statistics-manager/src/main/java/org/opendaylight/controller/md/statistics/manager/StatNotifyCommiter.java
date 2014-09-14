/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.yangtools.yang.binding.NotificationListener;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager
 *
 * StatNotifyCommiter
 * Definition Interface for notification implementer class rule
 * Interface represent a contract between RPC Device Notification
 * and Operational/DataStore commits.
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 28, 2014
 */
public interface StatNotifyCommiter<N extends NotificationListener> extends AutoCloseable, NotificationListener {


}

