/*
 * Copyright 2017-2022 Open Networking Foundation and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Segment descriptor test.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class JournalSegmentDescriptorTest {

  /**
   * Tests the segment descriptor methods.
   */
  @Test
  public void testFromBuffer() {
    final var buffer = new JournalSegmentDescriptor(2, 1025).asBuffer();

    final var descriptor = JournalSegmentDescriptor.fromBuffer(buffer);

    assertEquals(2, descriptor.id());
    assertEquals(1025, descriptor.index());
  }
}
