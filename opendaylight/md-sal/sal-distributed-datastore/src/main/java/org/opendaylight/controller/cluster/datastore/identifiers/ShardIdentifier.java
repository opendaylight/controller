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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

public final class ShardIdentifier implements WritableIdentifier {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // This pattern needs to remain in sync with toString(), which produces
    // strings with corresponding to "%s-shard-%s-%s"
    private static final Pattern PATTERN = Pattern.compile("(\\S+)-shard-(\\S+)-(\\S+)");

    private final String shardName;
    private final MemberName memberName;
    private final String type;
    private final String actorName;

    ShardIdentifier(final String shardName, final MemberName memberName, final String type) {
        this.shardName = requireNonNull(shardName, "shardName should not be null");
        this.memberName = requireNonNull(memberName, "memberName should not be null");
        this.type = requireNonNull(type, "type should not be null");

        actorName = memberName.getName() + "-shard-" + shardName + "-" + type;
    }

    public static ShardIdentifier create(final String shardName, final MemberName memberName, final String type) {
        return new ShardIdentifier(shardName, memberName, type);
    }

    public static ShardIdentifier fromShardIdString(final String shardIdString) {
        final Matcher matcher = PATTERN.matcher(shardIdString);
        checkArgument(matcher.matches(), "Invalid shard id \"%s\"", shardIdString);

        return new ShardIdentifier(matcher.group(2), MemberName.forName(matcher.group(1)), matcher.group(3));
    }

    public static ShardIdentifier readFrom(final DataInput in) throws IOException {
        final var shardName = in.readUTF();
        final var memberName = MemberName.readFrom(in);
        final var type = in.readUTF();
        return new ShardIdentifier(shardName, memberName, type);
    }

    public static Builder builder() {
        return new Builder();
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

    public String fullName() {
        return actorName;
    }

    @Override
    public void writeTo(final DataOutput out) throws IOException {
        out.writeUTF(shardName);
        memberName.writeTo(out);
        out.writeUTF(type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shardName, memberName, type);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof ShardIdentifier other
            && memberName.equals(other.memberName) && shardName.equals(other.shardName) && type.equals(other.type);
    }

    @Override
    public String toString() {
        // ensure the output of toString matches the pattern above
        return fullName();
    }

    @java.io.Serial
    private Object writeReplace() {
        return new SI(this);
    }

    public static final class Builder {
        private String shardName;
        private MemberName memberName;
        private String type;

        private Builder() {
            // Hidden on purpose
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
            final var matcher = PATTERN.matcher(shardId);
            if (matcher.matches()) {
                memberName = MemberName.forName(matcher.group(1));
                shardName = matcher.group(2);
                type = matcher.group(3);
            }
            return this;
        }

        public ShardIdentifier build() {
            return new ShardIdentifier(shardName, memberName, type);
        }
    }
}
