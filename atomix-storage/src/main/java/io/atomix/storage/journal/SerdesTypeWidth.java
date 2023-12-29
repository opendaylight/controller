/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Variadic encoding to type identifier.
 */
enum SerdesTypeWidth {
    BYTE(0, 0xFF) {
        @Override
        int readType(final DataInput input) throws IOException {
            return input.readUnsignedByte();
        }

        @Override
        void writeType(final DataOutput output, final int typeId) throws IOException {
            output.writeByte(typeId);
        }
    },
    SHORT(0, 0xFFFF) {
        @Override
        int readType(final DataInput input) throws IOException {
            return input.readUnsignedShort();
        }

        @Override
        void writeType(final DataOutput output, final int typeId) throws IOException {
            output.writeShort(typeId);
        }
    },
    INT(Integer.MIN_VALUE, Integer.MAX_VALUE) {
        @Override
        int readType(final DataInput input) throws IOException {
            return input.readInt();
        }

        @Override
        void writeType(final DataOutput output, final int typeId) throws IOException {
            output.writeInt(typeId);
        }
    };

    private final int min;
    private final int max;

    SerdesTypeWidth(final int min, final int max) {
        this.min = min;
        this.max = max;
    }

    static @NonNull SerdesTypeWidth forMaximum(final int max) {
        if (max <= 0xFF) {
            return BYTE;
        }
        if (max <= 0xFFFF) {
            return SHORT;
        }
        return INT;
    }

    final void checkRange(final int typeId) {
        if (typeId < min || typeId > max) {
            throw new IllegalArgumentException(this + " cannot encode " + typeId);
        }
    }

    abstract int readType(@NonNull DataInput input) throws IOException;

    abstract void writeType(@NonNull DataOutput output, int typeId) throws IOException;
}