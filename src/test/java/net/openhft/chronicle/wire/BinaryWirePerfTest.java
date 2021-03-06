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

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.bytes.NativeBytes.nativeBytes;

@RunWith(value = Parameterized.class)
public class BinaryWirePerfTest {
    final int testId;
    final boolean fixed;
    final boolean numericField;
    final boolean fieldLess;
    Bytes bytes = nativeBytes();

    public BinaryWirePerfTest(int testId, boolean fixed, boolean numericField, boolean fieldLess) {
        this.testId = testId;
        this.fixed = fixed;
        this.numericField = numericField;
        this.fieldLess = fieldLess;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{0, false, false, false},
                new Object[]{1, true, false, false},
                new Object[]{2, false, true, false},
                new Object[]{3, true, true, false},
                new Object[]{4, false, false, true},
                new Object[]{5, true, false, true}
        );
    }

    private Wire createBytes() {
        bytes.clear();
        if (testId == -1)
            return new RawWire(bytes);
        return new BinaryWire(bytes, fixed, numericField, fieldLess);
    }


    @Test
    public void wirePerf() throws StreamCorruptedException {
        System.out.println("TestId: " + testId + ", fixed: " + fixed + ", numberField: " + numericField + ", fieldLess: " + fieldLess);
        Wire wire = createBytes();
        MyTypes a = new MyTypes();
        for (int t = 0; t < 3; t++) {
            a.text.setLength(0);
            a.text.append("Hello World");
            wirePerf0(wire, a, new MyTypes(), t);
        }
    }

    private void wirePerf0(Wire wire, MyTypes a, MyTypes b, int t) throws StreamCorruptedException {
        long start = System.nanoTime();
        int runs = 500000;
        for (int i = 0; i < runs; i++) {
            wire.clear();
            a.b = (i & 1) != 0;
            a.d = i;
            a.i = i;
            a.l = i;
            a.writeMarshallable(wire);
            wire.flip();
            b.readMarshallable(wire);
        }
        long rate = (System.nanoTime() - start) / runs;
        System.out.printf("(vars) %,d : %,d ns avg, len= %,d%n", t, rate, wire.bytes().position());
    }


    @Test
    public void wirePerfInts() {
        System.out.println("TestId: " + testId + ", fixed: " + fixed + ", numberField: " + numericField + ", fieldLess: " + fieldLess);
        Wire wire = createBytes();
        MyType2 a = new MyType2();
        for (int t = 0; t < 3; t++) {
            wirePerf0(wire, a, new MyType2(), t);
        }
    }

    private void wirePerf0(Wire wire, MyType2 a, MyType2 b, int t) {
        long start = System.nanoTime();
        int runs = 1500000;
        for (int i = 0; i < runs; i++) {
            wire.clear();
            a.i = i;
            a.l = i;
            a.writeMarshallable(wire);
            wire.flip();
            b.readMarshallable(wire);
        }
        long rate = (System.nanoTime() - start) / runs;
        System.out.printf("(ints) %,d : %,d ns avg, len= %,d%n", t, rate, wire.bytes().position());
    }

    static class MyType2 implements Marshallable {
        int i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x;

        void i(int i) {
            this.i = i;
        }

        void j(int i) {
            this.j = i;
        }

        void k(int i) {
            this.k = i;
        }

        void l(int i) {
            this.l = i;
        }

        void m(int i) {
            this.m = i;
        }

        void n(int i) {
            this.n = i;
        }

        void o(int i) {
            this.o = i;
        }

        void p(int i) {
            this.p = i;
        }

        void q(int i) {
            this.q = i;
        }

        void r(int i) {
            this.r = i;
        }

        void s(int i) {
            this.s = i;
        }

        void t(int i) {
            this.t = i;
        }

        void u(int i) {
            this.u = i;
        }

        void v(int i) {
            this.v = i;
        }

        void w(int i) {
            this.w = i;
        }

        void x(int i) {
            this.x = i;
        }

        @Override
        public void writeMarshallable(WireOut wire) {
            wire.write(Fields.I).int32(i)
                    .write(Fields.J).int32(j)
                    .write(Fields.K).int32(k)
                    .write(Fields.L).int32(l)
                    .write(Fields.M).int32(m)
                    .write(Fields.N).int32(n)
                    .write(Fields.O).int32(o)
                    .write(Fields.P).int32(p)

                    .write(Fields.Q).int32(q)
                    .write(Fields.R).int32(r)
                    .write(Fields.S).int32(s)
                    .write(Fields.T).int32(t)
                    .write(Fields.U).int32(u)
                    .write(Fields.V).int32(v)
                    .write(Fields.W).int32(w)
                    .write(Fields.X).int32(v)
            ;
        }

        @Override
        public void readMarshallable(WireIn wire) {
            wire.read(Fields.I).int32(this::i)
                    .read(Fields.J).int32(this::j)
                    .read(Fields.K).int32(this::k)
                    .read(Fields.L).int32(this::l)
                    .read(Fields.M).int32(this::m)
                    .read(Fields.N).int32(this::n)
                    .read(Fields.O).int32(this::o)
                    .read(Fields.P).int32(this::p)
                    .read(Fields.Q).int32(this::q)
                    .read(Fields.R).int32(this::r)
                    .read(Fields.S).int32(this::s)
                    .read(Fields.T).int32(this::t)
                    .read(Fields.U).int32(this::u)
                    .read(Fields.V).int32(this::v)
                    .read(Fields.W).int32(this::w)
                    .read(Fields.X).int32(this::x)
            ;
        }

        enum Fields implements WireKey {
            I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X;

            @Override
            public int code() {
                return ordinal();
            }
        }
    }
}