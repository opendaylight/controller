package org.opendaylight.controller.versionapi.northbound;

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
