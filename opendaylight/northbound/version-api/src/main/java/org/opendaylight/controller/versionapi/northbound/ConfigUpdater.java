package org.opendaylight.controller.versionapi.northbound;
/**
* Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
import java.util.ArrayList;
import java.util.Dictionary;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class ConfigUpdater implements ManagedService{

    private Version v;

    @Override
    public void updated(Dictionary config) throws ConfigurationException {
        if (config == null){
            return;
        }
        if (v == null){
            v = new Version();
        }
        v.setVersion((String)config.get("org.opendaylight.controller.version"));
        v.setScmVersion((String)config.get("org.opendaylight.controller.build.scm.version"));
        v.setBuildUser((String)config.get("org.opendaylight.controller.build.user"));
        v.setBuildWorkspace((String)config.get("org.opendaylight.controller.build.workspace"));
        v.setBuildTimestamp((String)config.get("org.opendaylight.controller.build.timestamp"));
        v.setBuildMachine((String)config.get("org.opendaylight.controller.build.machine"));
    }

    public Version getVersion(){
        return this.v;
    }
    public ArrayList<Version> getVersions(){
        ArrayList<Version> v = new ArrayList<Version>();
        v.add(this.v);
        return v;
    }
}
