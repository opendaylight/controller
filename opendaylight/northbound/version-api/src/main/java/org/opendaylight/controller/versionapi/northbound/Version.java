package org.opendaylight.controller.versionapi.northbound;

/**
 * Copyright (c) 2014 Inocybe Technologies, and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
public class Version {
    private String version;
    private String scmVersion;
    private String buildUser;
    private String buildWorkspace;
    private String buildTimestamp;
    private String buildMachine;

    public Version() {
    }

    public Version(String version, String scmVersion, String buildUser,
            String buildWorkspace, String buildTimestamp, String buildMachine) {
        super();
        this.version = version;
        this.scmVersion = scmVersion;
        this.buildUser = buildUser;
        this.buildWorkspace = buildWorkspace;
        this.buildTimestamp = buildTimestamp;
        this.buildMachine = buildMachine;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScmVersion() {
        return scmVersion;
    }

    public void setScmVersion(String scmVersion) {
        this.scmVersion = scmVersion;
    }

    public String getBuildUser() {
        return buildUser;
    }

    public void setBuildUser(String buildUser) {
        this.buildUser = buildUser;
    }

    public String getBuildWorkspace() {
        return buildWorkspace;
    }

    public void setBuildWorkspace(String buildWorkspace) {
        this.buildWorkspace = buildWorkspace;
    }

    public String getBuildTimestamp() {
        return buildTimestamp;
    }

    public void setBuildTimestamp(String buildTimestamp) {
        this.buildTimestamp = buildTimestamp;
    }

    public String getBuildMachine() {
        return buildMachine;
    }

    public void setBuildMachine(String buildMachine) {
        this.buildMachine = buildMachine;
    }
}
