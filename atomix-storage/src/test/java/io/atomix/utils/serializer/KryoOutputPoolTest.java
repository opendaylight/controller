/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
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
package io.atomix.utils.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.esotericsoftware.kryo.io.Output;
import org.junit.jupiter.api.Test;

class KryoOutputPoolTest {
    private final KryoOutputPool kryoOutputPool = new KryoOutputPool();

    @Test
    void discardOutput() {
        final var result = new Output[2];
        kryoOutputPool.run(output -> {
            result[0] = output;
            return null;
        }, KryoOutputPool.MAX_POOLED_BUFFER_SIZE + 1);
        kryoOutputPool.run(output -> {
            result[1] = output;
            return null;
        }, 0);
        assertNotSame(result[0], result[1]);
    }

    @Test
    void recycleOutput() {
        final var result = new ByteArrayOutput[2];
        kryoOutputPool.run(output -> {
            output.writeInt(1);
            assertEquals(Integer.BYTES, output.position());
            result[0] = output;
            return null;
        }, 0);
        assertEquals(0, result[0].position());
        assertEquals(0, result[0].getByteArrayOutputStream().size());
        kryoOutputPool.run(output -> {
            assertEquals(0, output.position());
            result[1] = output;
            return null;
        }, 0);
        assertSame(result[0], result[1]);
    }
}
