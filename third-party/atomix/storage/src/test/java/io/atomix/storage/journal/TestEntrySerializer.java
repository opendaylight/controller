/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.ByteArraySerializer;

class TestEntrySerializer extends Serializer<TestEntry> {
    private static final ByteArraySerializer BA_SERIALIZER = new ByteArraySerializer();

    @Override
    public void write(Kryo kryo, Output output, TestEntry object) {
        kryo.writeObjectOrNull(output, object.bytes(), BA_SERIALIZER);
    }

    @Override
    public TestEntry read(Kryo kryo, Input input, Class<TestEntry> type) {
        return new TestEntry(kryo.readObjectOrNull(input, byte[].class, BA_SERIALIZER));
    }
}
