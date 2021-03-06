/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.IntValue;

import java.util.function.Supplier;

public class IntTextReference implements IntValue, Byteable {
    public static final byte[] template = "!!atomic { locked: false, value: 0000000000 }".getBytes();
    public static final int FALSE = ('f' << 24) | ('a' << 16) | ('l' << 8) | 's';
    public static final int TRUE = (' ' << 24) | ('t' << 16) | ('r' << 8) | 'u';
    static final int LOCKED = 19;
    static final int VALUE = 33;
    private static final int DIGITS = 10;
    private BytesStore bytes;
    private long offset;

    <T> T withLock(Supplier<T> call) {
        long valueOffset = offset + LOCKED;
        int value = bytes.readVolatileInt(valueOffset);
        if (value != FALSE && value != TRUE)
            throw new IllegalStateException();
        while (true) {
            if (bytes.compareAndSwapInt(valueOffset, FALSE, TRUE)) {
                T t = call.get();
                bytes.writeOrderedInt(valueOffset, FALSE);
                return t;
            }
        }
    }

    @Override
    public int getValue() {
        return withLock(() -> (int) bytes.parseLong(offset + VALUE));
    }

    @Override
    public void setValue(int value) {
        withLock(() -> bytes.append(offset + VALUE, value, DIGITS));
    }

    @Override
    public int getVolatileValue() {
        return getValue();
    }

    @Override
    public void setOrderedValue(int value) {
        setValue(value);
    }

    @Override
    public int addValue(int delta) {
        return withLock(() -> {
            long value = bytes.parseLong(offset + VALUE) + delta;
            bytes.append(offset + VALUE, value, DIGITS);
            return (int) value;
        });
    }

    @Override
    public int addAtomicValue(int delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(int expected, int value) {
        return withLock(() -> {
            if (bytes.parseLong(offset + VALUE) == expected) {
                bytes.append(offset + VALUE, value, DIGITS);
                return true;
            }
            return false;
        });
    }

    @Override
    public void bytesStore(BytesStore bytes, long offset, long length) {
        if (length != template.length) throw new IllegalArgumentException();
        this.bytes = bytes;
        this.offset = offset;
    }

    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long maxSize() {
        return template.length;
    }

    public static void write(Bytes bytes, int value) {
        long position = bytes.position();
        bytes.write(template);
        bytes.append(position+VALUE, value, DIGITS);
    }

    public String toString() { return "value: "+getValue(); }
}
