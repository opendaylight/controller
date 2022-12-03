/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.identifiers;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

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

    ShardIdentifier(final String shardName, final MemberName memberName, final String type) {
        this.shardName = requireNonNull(shardName, "shardName should not be null");
        this.memberName = requireNonNull(memberName, "memberName should not be null");
        this.type = requireNonNull(type, "type should not be null");

        fullName = memberName.getName() + "-shard-" + shardName + "-" + type;
    }

    public static ShardIdentifier create(final String shardName, final MemberName memberName, final String type) {
        return new ShardIdentifier(shardName, memberName, type);
    }

    public static ShardIdentifier fromShardIdString(final String shardIdString) {
        final Matcher matcher = PATTERN.matcher(shardIdString);
        checkArgument(matcher.matches(), "Invalid shard id \"%s\"", shardIdString);

        return new ShardIdentifier(matcher.group(2), MemberName.forName(matcher.group(1)), matcher.group(3));
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final var that = (ShardIdentifier) obj;
        return memberName.equals(that.memberName) && shardName.equals(that.shardName) && type.equals(that.type);
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

        public Builder shardName(final String newShardName) {
            shardName = newShardName;
            return this;
        }

        public Builder memberName(final MemberName newMemberName) {
            memberName = newMemberName;
            return this;
        }

        public Builder type(final String newType) {
            type = newType;
            return this;
        }

        public Builder fromShardIdString(final String shardId) {
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
