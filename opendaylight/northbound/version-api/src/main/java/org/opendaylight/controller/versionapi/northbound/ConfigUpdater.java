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
        v.setName((String)config.get("name"));
        v.setVersion((String)config.get("version"));
        v.setStream((String)config.get("stream"));
        v.setTimestamp((String)config.get("timestamp"));
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
