/*
 * Copyright 2014 Higher Frequency Trading
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

import net.openhft.chronicle.bytes.NativeBytesStore;
import org.junit.Assert;
import org.junit.Test;

public class TextLongReferenceTest {

    @Test
    public void testSetValue() throws Exception {
        final TextLongReference value = new TextLongReference();
        try (NativeBytesStore bytesStore = NativeBytesStore.nativeStoreWithFixedCapacity(value.maxSize())) {
            value.bytesStore(bytesStore, 0, value.maxSize());
            int expected = 10;
            value.setValue(expected);

            long l = bytesStore.parseLong(TextLongReference.VALUE);
            System.out.println(l);

//        System.out.println(Bytes.toHexString(bytes,33, bytes.limit() - 33));

            Assert.assertEquals(expected, value.getValue());
        }
    }
}