/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

/**
 * A single frame.
 */
record Frame(long entryId, int length, int frameNo, int crc32c, byte[] data) {
    // Envisioned layout.
    //
    //
    // u64    entryId   (0L indicates a Manifest entry, -1L indicates next unallocated entry)
    // u32    length    (including header, set to -1L when first allocated, flipped to real length once complete)
    // u32    frameNo   (-1L indicates last frame of an entry)
    // u32    crc32c    (only valid if length has been observed as > 0)
    // byte[] data      (data.length == length - 20 bytes)
    //
    //
    // This gives us minimum entry size of 24 bytes, with 4 bytes available in the smallest possible frames.
    // We increase that size to 32 bytes just to keep an even alignment and 12 bytes in smallest frame.
    //
    // 12 bytes is notably more than the 8 bytes required for long/double, which are the largest reads where we do not
    // receive data into a byte array. We can therefore assume that such a read does not involve more than two frames.
    //
    // Order of writes:
    // - set length to -1L
    // - set entryId value
    // - write data, including padding to frame alignment
    // - write -1L after alignment (e.g. the next entry's entryId == -1L)
    // - write frameNo
    // - write crc32
    // - write real length with release barrier
    // - update pointer to next with release barrier
    // ---> readers see the complete entry
    //
    // FIXME: audit the above for thread safety (i.e. safe publish)
    // FIXME: audit the above for synchronization points (i.e. force())
}
