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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.wire.util.BooleanConsumer;
import net.openhft.chronicle.wire.util.ByteConsumer;
import net.openhft.chronicle.wire.util.FloatConsumer;
import net.openhft.chronicle.wire.util.ShortConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

/**
 * Created by peter.lawrey on 15/01/15.
 */
public class QueryWire implements Wire, InternalWireIn {
    private static final Logger LOG =
            LoggerFactory.getLogger(QueryWire.class);

    final Bytes<?> bytes;
    final TextValueOut valueOut = new TextValueOut();
    final ValueIn valueIn = new TextValueIn();

    boolean ready;

    public QueryWire(Bytes bytes) {
        this.bytes = bytes;
    }

    public static String asText(Wire wire) {
        QueryWire tw = new QueryWire(nativeBytes());
        wire.copyTo(tw);
        tw.flip();
        wire.flip();
        return tw.toString();
    }

    public String toString() {
        return bytes.toString();
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public void copyTo(@NotNull WireOut wire) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueIn read() {
        readField(Wires.acquireStringBuilder());
        return valueIn;
    }

    private StringBuilder readField(StringBuilder sb) {
        consumeWhiteSpace();
        bytes.parseUTF(sb, QueryStopCharTesters.QUERY_FIELD_NAME);
        if (rewindAndRead() == '&')
            bytes.skip(-1);
        return sb;
    }

    void consumeWhiteSpace() {
        int codePoint = peekCode();
        while (Character.isWhitespace(codePoint)) {
            bytes.skip(1);
            codePoint = peekCode();
        }
    }

    int peekCode() {
        return bytes.peekUnsignedByte();
    }

    /**
     * returns true if the next string is {@code str}
     *
     * @param source string
     * @return true if the strings are the same
     */
    private boolean peekStringIgnoreCase(@NotNull final String source) {
        if (source.isEmpty())
            return true;

        if (bytes.remaining() < 1)
            return false;

        long pos = bytes.position();

        try {
            for (int i = 0; i < source.length(); i++) {
                if (Character.toLowerCase(source.charAt(i)) != Character.toLowerCase(bytes.readByte()))
                    return false;
            }
        } finally {
            bytes.position(pos);
        }

        return true;
    }

    private int readCode() {
        if (bytes.remaining() < 1)
            return -1;
        return bytes.readUnsignedByte();
    }

    public static <ACS extends Appendable & CharSequence> void unescape(ACS sb) {
        int end = 0;
        for (int i = 0; i < sb.length(); i++) {
            char ch = sb.charAt(i);
            if (ch == '%' && i < sb.length() - 1) {
                char ch3 = sb.charAt(++i);
                char ch4 = sb.charAt(++i);
                ch = (char) Integer.parseInt("" + ch3 + ch4, 16);
            }
            BytesUtil.setCharAt(sb, end++, ch);
        }
        BytesUtil.setLength(sb, end);
    }

    @Override
    public ValueIn read(@NotNull WireKey key) {
        long position = bytes.position();
        StringBuilder sb = readField(Wires.acquireStringBuilder());
        if (sb.length() == 0 || StringUtils.isEqual(sb, key.name()))
            return valueIn;
        bytes.position(position);
        throw new UnsupportedOperationException("Unordered fields not supported yet. key=" + key
                .name() + ", was=" + sb + ", data='" + sb + "'");
    }

    @Override
    public ValueIn read(@NotNull StringBuilder name) {
        consumeWhiteSpace();
        readField(name);
        return valueIn;
    }

    @Override
    public ValueIn getValueIn() {
        return valueIn;
    }

    @Override
    public Wire readComment(@NotNull StringBuilder s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flip() {
        bytes.flip();
    }

    @Override
    public void clear() {
        bytes.clear();
    }

    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    @Override
    public ValueOut write() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueOut write(WireKey key) {
        return valueOut.write(key);
    }

    @Override
    public ValueOut writeValue() {
        return valueOut;
    }

    @Override
    public ValueOut getValueOut() {
        return valueOut;
    }

    @Override
    public Wire writeComment(CharSequence s) {
        return this;
    }

    @Override
    public WireOut addPadding(int paddingToAdd) {
        return this;
    }

    int rewindAndRead() {
        return bytes.readUnsignedByte(bytes.position() - 1);
    }

    class TextValueOut implements ValueOut {
        String sep = "";
        CharSequence fieldName = null;

        void prependSeparator() {
            bytes.append(sep);
            sep = "";
            if (fieldName != null) {
                bytes.append(fieldName).append("=");
                fieldName = null;
            }
        }

        @Override
        public ValueOut leaf() {
            return this;
        }

        @Override
        public WireOut wireOut() {
            return QueryWire.this;
        }

        public void elementSeparator() {
            sep = "&";
        }

        @Override
        public WireOut bool(Boolean flag) {
            if (flag != null) {
                prependSeparator();
                bytes.append(flag ? "true" : "false");
                elementSeparator();
            }
            return QueryWire.this;
        }

        @Override
        public WireOut text(CharSequence s) {
            if (s != null) {
                prependSeparator();
                bytes.append(s);
                elementSeparator();
            }
            return QueryWire.this;
        }

        @Override
        public WireOut int8(byte i8) {
            prependSeparator();
            bytes.append(i8);
            elementSeparator();
            return QueryWire.this;
        }

        @Override
        public WireOut bytes(Bytes fromBytes) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireOut rawBytes(byte[] value) {
            if (value != null) {
                prependSeparator();
                bytes.write(value);
                elementSeparator();
            }
            return QueryWire.this;
        }

        private boolean isText(Bytes fromBytes) {
            for (long i = fromBytes.position(); i < fromBytes.readLimit(); i++) {
                int ch = fromBytes.readUnsignedByte(i);
                if ((ch < ' ' && ch != '\t') || ch == '&' || ch >= 127)
                    return false;
            }
            return true;
        }

        @Override
        public ValueOut writeLength(long remaining) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut bytes(byte[] byteArray) {
            prependSeparator();
            bytes.append(Base64.getEncoder().encodeToString(byteArray));
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut uint8checked(int u8) {
            prependSeparator();
            bytes.append(u8);
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut int16(short i16) {
            prependSeparator();
            bytes.append(i16);
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut uint16checked(int u16) {
            prependSeparator();
            bytes.append(u16);
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut utf8(int codepoint) {
            prependSeparator();
            StringBuilder sb = Wires.acquireStringBuilder();
            sb.appendCodePoint(codepoint);
            text(sb);
            return QueryWire.this;
        }

        @Override
        public WireOut int32(int i32) {
            prependSeparator();
            bytes.append(i32);
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut uint32checked(long u32) {
            prependSeparator();
            bytes.append(u32);
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut int64(long i64) {
            prependSeparator();
            bytes.append(i64);
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut int64array(long capacity) {
            TextLongArrayReference.write(bytes, capacity);
            return QueryWire.this;
        }

        @Override
        public WireOut float32(float f) {
            prependSeparator();
            bytes.append(f);
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut float64(double d) {
            prependSeparator();
            bytes.append(d);
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut time(LocalTime localTime) {
            prependSeparator();
            bytes.append(localTime.toString());
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut zonedDateTime(ZonedDateTime zonedDateTime) {
            prependSeparator();
            bytes.append(zonedDateTime.toString());
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut date(LocalDate localDate) {
            prependSeparator();
            bytes.append(localDate.toString());
            elementSeparator();

            return QueryWire.this;
        }

        @Override
        public WireOut type(CharSequence typeName) {
            prependSeparator();
            bytes.append(typeName);
            sep = " ";
            return QueryWire.this;
        }

        @Override
        public WireOut typeLiteral(@NotNull CharSequence type) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @NotNull Class type) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public WireOut uuid(UUID uuid) {
            prependSeparator();
            bytes.append(sep).append(uuid.toString());
            elementSeparator();
            return QueryWire.this;
        }

        @Override
        public WireOut int32forBinding(int value) {
            prependSeparator();
            IntTextReference.write(bytes, value);
            elementSeparator();
            return QueryWire.this;
        }

        @Override
        public WireOut int64forBinding(long value) {
            prependSeparator();
            TextLongReference.write(bytes, value);
            elementSeparator();
            return QueryWire.this;
        }

        @Override
        public WireOut sequence(Consumer<ValueOut> writer) {
            prependSeparator();
            pushState();
            bytes.append("[");
            sep = ",";
            long pos = bytes.position();
            writer.accept(this);
            if (pos != bytes.position())
                bytes.append(",");

            popState();
            bytes.append("]");
            elementSeparator();
            return QueryWire.this;
        }

        private void popState() {
        }

        private void pushState() {
        }

        @Override
        public WireOut marshallable(WriteMarshallable object) {
            pushState();

            prependSeparator();
            bytes.append("{");
            sep = ",";

            object.writeMarshallable(QueryWire.this);

            popState();

            bytes.append('}');
            elementSeparator();
            return QueryWire.this;
        }

        @Override
        public WireOut map(@NotNull final Map map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map) {
            throw new UnsupportedOperationException();
        }

        public ValueOut write() {
            bytes.append(sep).append("\"\": ");
            sep = "";
            return this;
        }

        public ValueOut write(WireKey key) {
            fieldName = key.name();
            return this;
        }
    }

    class TextValueIn implements ValueIn {
        @NotNull
        @Override
        public WireIn bool(@NotNull BooleanConsumer flag) {
            consumeWhiteSpace();

            StringBuilder sb = Wires.acquireStringBuilder();
            if (textTo(sb) == null) {
                flag.accept(null);
                return QueryWire.this;
            }

            flag.accept(StringUtils.isEqual(sb, "true"));
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn text(@NotNull Consumer<String> s) {
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            s.accept(sb.toString());
            return QueryWire.this;
        }

        @Override
        public String text() {
            return StringUtils.toString(textTo(Wires.acquireStringBuilder()));
        }

        @Nullable
        @Override
        public <ACS extends Appendable & CharSequence> ACS textTo(@NotNull ACS a) {
            consumeWhiteSpace();
            bytes.parseUTF(a, QueryStopCharTesters.QUERY_VALUE);
            return a;
        }

        @NotNull
        @Override
        public WireIn int8(@NotNull ByteConsumer i) {
            consumeWhiteSpace();
            i.accept((byte) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn bytes(@NotNull Bytes toBytes) {
            return bytes(wi -> toBytes.write(wi.bytes()));
        }

        @NotNull
        public WireIn bytes(@NotNull Consumer<WireIn> bytesConsumer) {
            throw new UnsupportedOperationException("todo");
        }

        public byte[] bytes() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn wireIn() {
            return QueryWire.this;
        }

        @Override
        public long readLength() {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn uint8(@NotNull ShortConsumer i) {
            consumeWhiteSpace();
            i.accept((short) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn int16(@NotNull ShortConsumer i) {
            consumeWhiteSpace();
            i.accept((short) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn uint16(@NotNull IntConsumer i) {
            consumeWhiteSpace();
            i.accept((int) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn int32(@NotNull IntConsumer i) {
            consumeWhiteSpace();
            i.accept((int) bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn uint32(@NotNull LongConsumer i) {
            consumeWhiteSpace();
            i.accept(bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn int64(@NotNull LongConsumer i) {
            consumeWhiteSpace();
            i.accept(bytes.parseLong());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn float32(@NotNull FloatConsumer v) {
            consumeWhiteSpace();
            v.accept((float) bytes.parseDouble());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn float64(@NotNull DoubleConsumer v) {
            consumeWhiteSpace();
            v.accept(bytes.parseDouble());
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn time(@NotNull Consumer<LocalTime> localTime) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            localTime.accept(LocalTime.parse(sb.toString()));
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn zonedDateTime(@NotNull Consumer<ZonedDateTime> zonedDateTime) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            zonedDateTime.accept(ZonedDateTime.parse(sb.toString()));
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn date(@NotNull Consumer<LocalDate> localDate) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            localDate.accept(LocalDate.parse(sb.toString()));
            return QueryWire.this;
        }

        @Override
        public boolean hasNext() {
            return bytes.remaining() > 0;
        }

        @Override
        public boolean hasNextSequenceItem() {
            consumeWhiteSpace();
            int ch = peekCode();
            if (ch == ',') {
                bytes.skip(1);
                return true;
            }
            return ch != ']';
        }

        @Override
        public WireIn uuid(@NotNull Consumer<UUID> uuid) {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            textTo(sb);
            uuid.accept(UUID.fromString(sb.toString()));
            return QueryWire.this;
        }

        @Override
        public WireIn int64array(@Nullable LongArrayValues values, @NotNull Consumer<LongArrayValues> setter) {
            consumeWhiteSpace();
            if (!(values instanceof TextLongArrayReference)) {
                setter.accept(values = new TextLongArrayReference());
            }
            Byteable b = (Byteable) values;
            long length = TextLongArrayReference.peakLength(bytes, bytes.position());
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            return QueryWire.this;
        }

        @Override
        public WireIn int64(LongValue value, @NotNull Consumer<LongValue> setter) {
            consumeWhiteSpace();
            if (!(value instanceof TextLongReference)) {
                setter.accept(value = new TextLongReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            consumeWhiteSpace();
            if (peekCode() == ',')
                bytes.skip(1);
            return QueryWire.this;
        }

        @Override
        public WireIn int32(IntValue value, @NotNull Consumer<IntValue> setter) {
            if (!(value instanceof IntTextReference)) {
                setter.accept(value = new IntTextReference());
            }
            Byteable b = (Byteable) value;
            long length = b.maxSize();
            b.bytesStore(bytes, bytes.position(), length);
            bytes.skip(length);
            consumeWhiteSpace();
            if (peekCode() == ',')
                bytes.skip(1);
            return QueryWire.this;
        }

        @Override
        public WireIn sequence(@NotNull Consumer<ValueIn> reader) {
            consumeWhiteSpace();
            int code = readCode();
            if (code != '[')
                throw new IORuntimeException("Unsupported type " + (char) code + " (" + code + ")");

            reader.accept(QueryWire.this.valueIn);

            consumeWhiteSpace();
            code = peekCode();
            if (code != ']')
                throw new IORuntimeException("Expected a ] but got " + (char) code + " (" + code + ")");

            return QueryWire.this;
        }

        @Override
        public <T> T applyToMarshallable(Function<WireIn, T> marshallableReader) {
            throw new UnsupportedOperationException("todo");
        }

        @NotNull
        @Override
        public WireIn type(@NotNull StringBuilder s) {
            consumeWhiteSpace();
            bytes.parseUTF(s, QueryStopCharTesters.QUERY_VALUE);
            return QueryWire.this;
        }

        @Override
        public WireIn typeLiteral(@NotNull Consumer<CharSequence> classNameConsumer) {
            StringBuilder sb = Wires.acquireStringBuilder();
            type(sb);
            classNameConsumer.accept(sb);
            return QueryWire.this;
        }

        @NotNull
        @Override
        public WireIn marshallable(@NotNull ReadMarshallable object) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public <K, V> Map<K, V> map(@NotNull final Class<K> kClazz,
                                    @NotNull final Class<V> vClass,
                                    @NotNull final Map<K, V> usingMap) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public <K extends ReadMarshallable, V extends ReadMarshallable> void typedMap(@NotNull Map<K, V> usingMap) {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public boolean bool() {
            consumeWhiteSpace();
            StringBuilder sb = Wires.acquireStringBuilder();
            if (textTo(sb) == null)
                throw new NullPointerException("value is null");

            return StringUtils.isEqual(sb, "true");
        }

        public byte int8() {
            long l = int64();
            if (l > Byte.MAX_VALUE || l < Byte.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Byte.MAX_VALUE/MIN_VALUE");
            return (byte) l;
        }

        public short int16() {
            long l = int64();
            if (l > Short.MAX_VALUE || l < Short.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Short.MAX_VALUE/MIN_VALUE");
            return (short) l;
        }

        public int int32() {
            long l = int64();
            if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE)
                throw new IllegalStateException("value=" + l + ", is greater or less than Integer.MAX_VALUE/MIN_VALUE");
            return (int) l;
        }

        public int uint16() {
            long l = int64();
            if (l > Integer.MAX_VALUE || l < 0)
                throw new IllegalStateException("value=" + l + ", is greater or less than Integer" +
                        ".MAX_VALUE/ZERO");
            return (int) l;
        }

        @Override
        public long int64() {
            consumeWhiteSpace();
            return bytes.parseLong();
        }

        @Override
        public double float64() {
            throw new UnsupportedOperationException("todo");
        }

        @Override
        public float float32() {
            throw new UnsupportedOperationException("todo");
        }

        /**
         * @return true if !!null, if {@code true} reads the !!null up to the next STOP, if {@code
         * false} no  data is read  ( data is only peaked if {@code false} )
         */
        public boolean isNull() {
            consumeWhiteSpace();

            if (peekStringIgnoreCase("!!null ")) {
                bytes.skip("!!null ".length());
                // discard the text after it.
                textTo(Wires.acquireStringBuilder());
                return true;
            }

            return false;
        }

        @Override
        @Nullable
        public <E> E object(@Nullable E using,
                            @NotNull Class<E> clazz) {
            consumeWhiteSpace();

            if (isNull())
                return null;

            if (byte[].class.isAssignableFrom(clazz))
                return (E) bytes();

            if (Marshallable.class.isAssignableFrom(clazz)) {
                final E v;
                if (using == null)
                    v = OS.memory().allocateInstance(clazz);
                else
                    v = using;

                valueIn.marshallable((Marshallable) v);
                return v;

            } else if (StringBuilder.class.isAssignableFrom(clazz)) {
                StringBuilder builder = (using == null)
                        ? Wires.acquireStringBuilder()
                        : (StringBuilder) using;
                valueIn.textTo(builder);
                return using;

            } else if (CharSequence.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) valueIn.text();

            } else if (Long.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Long) valueIn.int64();

            } else if (Double.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Double) valueIn.float64();

            } else if (Integer.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Integer) valueIn.int32();

            } else if (Float.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Float) valueIn.float32();

            } else if (Short.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Short) valueIn.int16();

            } else if (Character.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                final String text = valueIn.text();
                if (text == null || text.length() == 0)
                    return null;
                return (E) (Character) text.charAt(0);

            } else if (Byte.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                return (E) (Byte) valueIn.int8();

            } else if (Map.class.isAssignableFrom(clazz)) {
                //noinspection unchecked
                final Map result = new HashMap();
                valueIn.map(result);
                return (E) result;

            } else {
                throw new IllegalStateException("unsupported type=" + clazz);
            }
        }
    }

    enum QueryStopCharTesters implements StopCharTester {
        QUERY_FIELD_NAME {
            @Override
            public boolean isStopChar(int ch) throws IllegalStateException {
                return ch == '&' || ch == '=' || ch < 0;
            }
        },
        QUERY_VALUE {
            @Override
            public boolean isStopChar(int ch) throws IllegalStateException {
                return ch == '&' || ch < 0;
            }
        }
    }
}
