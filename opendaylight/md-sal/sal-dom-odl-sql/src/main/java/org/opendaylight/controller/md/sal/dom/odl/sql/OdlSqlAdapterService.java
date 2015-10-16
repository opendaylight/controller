/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.odl.sql;

import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.odl.sql.rev151016.OdlSqlService;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public interface OdlSqlAdapterService extends OdlSqlService{
    public ODLSQLAdapter getAdapter();
}
