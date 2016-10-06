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
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class ShardIdentifier {
    // This pattern needs to remain in sync with toString(), which produces
    // strings with corresponding to "%s-shard-%s-%s"
    private static final Pattern PATTERN = Pattern.compile("(\\S+)-shard-(\\S+)-(\\S+)");

    private final String shardName;
    private final MemberName memberName;
    private final String type;
    private final String fullName;

    private ShardIdentifier(String shardName, MemberName memberName, String type) {
        this.shardName = Preconditions.checkNotNull(shardName, "shardName should not be null");
        this.memberName = Preconditions.checkNotNull(memberName, "memberName should not be null");
        this.type = Preconditions.checkNotNull(type, "type should not be null");

        fullName = memberName.getName() + "-shard-" + shardName + "-" + type;
    }

    public static ShardIdentifier create(final String shardName, final MemberName memberName, final String type) {
        return new ShardIdentifier(shardName, memberName, type);
    }

    public static ShardIdentifier fromShardIdString(final String shardIdString) {
        final Matcher matcher = PATTERN.matcher(shardIdString);
        Preconditions.checkArgument(matcher.matches(), "Invalid shard id \"%s\"", shardIdString);

        return new ShardIdentifier(matcher.group(2), MemberName.forName(matcher.group(1)), matcher.group(3));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        ShardIdentifier that = (ShardIdentifier) obj;

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

    @Override
    public String toString() {
        // ensure the output of toString matches the pattern above
        return fullName;
    }

    public String getShardName() {
        return shardName;
    }

    public MemberName getMemberName() {
        return memberName;
    }

    public String getType() {
        return type;
    }

    public static class Builder {
        private String shardName;
        private MemberName memberName;
        private String type;

        public ShardIdentifier build() {
            return new ShardIdentifier(shardName, memberName, type);
        }

        public Builder shardName(String newShardName) {
            this.shardName = newShardName;
            return this;
        }

        public Builder memberName(MemberName newMemberName) {
            this.memberName = newMemberName;
            return this;
        }

        public Builder type(String newType) {
            this.type = newType;
            return this;
        }

        public Builder fromShardIdString(String shardId) {
            Matcher matcher = PATTERN.matcher(shardId);

            if (matcher.matches()) {
                memberName = MemberName.forName(matcher.group(1));
                shardName = matcher.group(2);
                type = matcher.group(3);
            }
            return this;
        }
    }
}
