/*
 * Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *   disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote product
 *   derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.atomix.utils.serializer;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import java.nio.ByteBuffer;

/**
 * A Kryo-4.0.3 ByteBufferInput adapted to deal with
 * <a href="https://github.com/EsotericSoftware/kryo/issues/505">issue 505</a>.
 *
 * @author Roman Levenstein &lt;romixlev@gmail.com&gt;
 * @author Robert Varga
 */
@SuppressWarnings("all")
public final class Kryo505ByteBufferInput extends ByteBufferInput {
	Kryo505ByteBufferInput (ByteBuffer buffer) {
		super(buffer);
	}

	@Override
	public String readString () {
		niobuffer.position(position);
		int available = require(1);
		position++;
		int b = niobuffer.get();
		if ((b & 0x80) == 0) return readAscii(); // ASCII.
		// Null, empty, or UTF8.
		int charCount = available >= 5 ? readUtf8Length(b) : readUtf8Length_slow(b);
		switch (charCount) {
		case 0:
			return null;
		case 1:
			return "";
		}
		charCount--;
		if (chars.length < charCount) chars = new char[charCount];
		readUtf8(charCount);
		return new String(chars, 0, charCount);
	}

	private int readUtf8Length (int b) {
		int result = b & 0x3F; // Mask all but first 6 bits.
		if ((b & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
			position++;
			b = niobuffer.get();
			result |= (b & 0x7F) << 6;
			if ((b & 0x80) != 0) {
				position++;
				b = niobuffer.get();
				result |= (b & 0x7F) << 13;
				if ((b & 0x80) != 0) {
					position++;
					b = niobuffer.get();
					result |= (b & 0x7F) << 20;
					if ((b & 0x80) != 0) {
						position++;
						b = niobuffer.get();
						result |= (b & 0x7F) << 27;
					}
				}
			}
		}
		return result;
	}

	private int readUtf8Length_slow (int b) {
		int result = b & 0x3F; // Mask all but first 6 bits.
		if ((b & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
			require(1);
			position++;
			b = niobuffer.get();
			result |= (b & 0x7F) << 6;
			if ((b & 0x80) != 0) {
				require(1);
				position++;
				b = niobuffer.get();
				result |= (b & 0x7F) << 13;
				if ((b & 0x80) != 0) {
					require(1);
					position++;
					b = niobuffer.get();
					result |= (b & 0x7F) << 20;
					if ((b & 0x80) != 0) {
						require(1);
						position++;
						b = niobuffer.get();
						result |= (b & 0x7F) << 27;
					}
				}
			}
		}
		return result;
	}

	private void readUtf8 (int charCount) {
		char[] chars = this.chars;
		// Try to read 7 bit ASCII chars.
		int charIndex = 0;
		int count = Math.min(require(1), charCount);
		int position = this.position;
		int b;
		while (charIndex < count) {
			position++;
			b = niobuffer.get();
			if (b < 0) {
				position--;
				break;
			}
			chars[charIndex++] = (char)b;
		}
		this.position = position;
		// If buffer didn't hold all chars or any were not ASCII, use slow path for remainder.
		if (charIndex < charCount) {
			niobuffer.position(position);
			readUtf8_slow(charCount, charIndex);
		}
	}

	private void readUtf8_slow (int charCount, int charIndex) {
		char[] chars = this.chars;
		while (charIndex < charCount) {
			if (position == limit) require(1);
			position++;
			int b = niobuffer.get() & 0xFF;
			switch (b >> 4) {
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				chars[charIndex] = (char)b;
				break;
			case 12:
			case 13:
				if (position == limit) require(1);
				position++;
				chars[charIndex] = (char)((b & 0x1F) << 6 | niobuffer.get() & 0x3F);
				break;
			case 14:
				require(2);
				position += 2;
				int b2 = niobuffer.get();
				int b3 = niobuffer.get();
				chars[charIndex] = (char)((b & 0x0F) << 12 | (b2 & 0x3F) << 6 | b3 & 0x3F);
				break;
			}
			charIndex++;
		}
	}

	private String readAscii () {
		int end = position;
		int start = end - 1;
		int limit = this.limit;
		int b;
		do {
			if (end == limit) return readAscii_slow();
			end++;
			b = niobuffer.get();
		} while ((b & 0x80) == 0);
		int count = end - start;
		byte[] tmp = new byte[count];
		niobuffer.position(start);
		niobuffer.get(tmp);
		tmp[count - 1] &= 0x7F;  // Mask end of ascii bit.
		String value = new String(tmp, 0, 0, count);
		position = end;
		niobuffer.position(position);
		return value;
	}

	private String readAscii_slow () {
		position--; // Re-read the first byte.
		// Copy chars currently in buffer.
		int charCount = limit - position;
		if (charCount > chars.length) chars = new char[charCount * 2];
		char[] chars = this.chars;
		for (int i = position, ii = 0, n = limit; i < n; i++, ii++)
			chars[ii] = (char)niobuffer.get(i);
		position = limit;
		// Copy additional chars one by one.
		while (true) {
			require(1);
			position++;
			int b = niobuffer.get();
			if (charCount == chars.length) {
				char[] newChars = new char[charCount * 2];
				System.arraycopy(chars, 0, newChars, 0, charCount);
				chars = newChars;
				this.chars = newChars;
			}
			if ((b & 0x80) == 0x80) {
				chars[charCount++] = (char)(b & 0x7F);
				break;
			}
			chars[charCount++] = (char)b;
		}
		return new String(chars, 0, charCount);
	}

	@Override
	public StringBuilder readStringBuilder () {
		niobuffer.position(position);
		int available = require(1);
		position++;
		int b = niobuffer.get();
		if ((b & 0x80) == 0) return new StringBuilder(readAscii()); // ASCII.
		// Null, empty, or UTF8.
		int charCount = available >= 5 ? readUtf8Length(b) : readUtf8Length_slow(b);
		switch (charCount) {
		case 0:
			return null;
		case 1:
			return new StringBuilder("");
		}
		charCount--;
		if (chars.length < charCount) chars = new char[charCount];
		readUtf8(charCount);
		StringBuilder builder = new StringBuilder(charCount);
		builder.append(chars, 0, charCount);
		return builder;
	}
}
