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
package net.openhft.chronicle.wire.util;

import java.util.Comparator;

public enum CharSequenceComparator implements Comparator<CharSequence> {
    INSTANCE;

    @Override
    public int compare(CharSequence o1, CharSequence o2) {
        int cmp = Integer.compare(o1.length(), o2.length());
        if (cmp != 0)
            return cmp;
        for (int i = 0, len = o1.length(); i < len; i++) {
            cmp = Character.compare(o1.charAt(i), o2.charAt(i));
            if (cmp != 0)
                return cmp;
        }
        return cmp;
    }
}
