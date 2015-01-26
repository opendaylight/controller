/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.identifiers;

import com.google.common.base.Preconditions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShardIdentifier {
    // This pattern needs to remain in sync with toString(), which produces
    // strings with corresponding to "%s-shard-%s-%s"
    private static final Pattern PATTERN = Pattern.compile("(\\S+)-shard-(\\S+)-(\\S+)");

    private final String shardName;
    private final String memberName;
    private final String type;

    public ShardIdentifier(String shardName, String memberName, String type) {

        Preconditions.checkNotNull(shardName, "shardName should not be null");
        Preconditions.checkNotNull(memberName, "memberName should not be null");
        Preconditions.checkNotNull(type, "type should not be null");

        this.shardName = shardName;
        this.memberName = memberName;
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

        ShardIdentifier that = (ShardIdentifier) o;

        if (!memberName.equals(that.memberName)) {
            return false;
        }
        if (!shardName.equals(that.shardName)) {
            return false;
        }
        if (!type.equals(that.type)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = shardName.hashCode();
        result = 31 * result + memberName.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override public String toString() {
        //ensure the output of toString matches the pattern above
        return new StringBuilder(memberName)
                    .append("-shard-")
                    .append(shardName)
                    .append("-")
                    .append(type)
                    .toString();
    }

    public static Builder builder(){
        return new Builder();
    }

    public String getShardName() {
        return shardName;
    }

    public String getMemberName() {
        return memberName;
    }

    public String getType() {
        return type;
    }

    public static class Builder {
        private String shardName;
        private String memberName;
        private String type;

        public ShardIdentifier build(){
            return new ShardIdentifier(shardName, memberName, type);
        }

        public Builder shardName(String shardName){
            this.shardName = shardName;
            return this;
        }

        public Builder memberName(String memberName){
            this.memberName = memberName;
            return this;
        }

        public Builder type(String type){
            this.type = type;
            return this;
        }

        public Builder fromShardIdString(String shardId){
            Matcher matcher = PATTERN.matcher(shardId);

            if (matcher.matches()) {
                memberName = matcher.group(1);
                shardName = matcher.group(2);
                type = matcher.group(3);
            }
            return this;
        }
    }
}
