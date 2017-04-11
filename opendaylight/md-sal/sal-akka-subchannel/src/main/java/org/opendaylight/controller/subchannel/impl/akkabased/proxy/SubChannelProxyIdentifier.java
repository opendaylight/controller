/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.impl.akkabased.proxy;

/**
 * Created by HanJie on 2017/2/6.
 *
 * @author Han Jie
 */
public class SubChannelProxyIdentifier {
    private final String type;

    public SubChannelProxyIdentifier(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SubChannelProxyIdentifier that = (SubChannelProxyIdentifier) o;

        if (!type.equals(that.type)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("subchannel-proxy-").append(type);

        return builder.toString();
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {
        private String type;
        public Builder type(String type){
            this.type = type;
            return this;
        }

        public SubChannelProxyIdentifier build(){
            return new SubChannelProxyIdentifier(this.type);
        }

    }
}
