/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.swagger;

import java.util.List;

/**
 * Implementation of swagger spec (see <a href=
 * "https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md#523-operation-object"
 * > https://github.com/wordnik/swagger-spec/blob/master/versions/1.2.md#523-
 * operation-object</a>)
 */
public class Operation {
    private String method;
    private String summary;
    private String notes;
    private String type;
    private String nickname;
    private List<String> consumes;
    private List<Parameter> parameters;
    private List<ResponseMessage> responseMessages;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public List<String> getConsumes() {
        return consumes;
    }

    public void setConsumes(List<String> consumes) {
        this.consumes = consumes;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public List<ResponseMessage> getResponseMessages() {
        return responseMessages;
    }

    public void setResponseMessages(List<ResponseMessage> responseMessages) {
        this.responseMessages = responseMessages;
    }
}
