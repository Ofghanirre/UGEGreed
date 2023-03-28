package fr.uge.ugegreed.tests;

import fr.uge.ugegreed.readers.BaseReader;
import fr.uge.ugegreed.readers.Reader;
import fr.uge.ugegreed.readers.StringReader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BaseReaderTest {

    @Test
    public void simpleInt() {
        int integer= 42;
        var bb = ByteBuffer.allocate(Integer.BYTES);
        bb.putInt(integer);

        var intReader = BaseReader.intReader();
        var state = intReader.process(bb);
        var result = intReader.get();

        assertEquals(Reader.ProcessStatus.DONE, state);
        assertEquals(result, integer);
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void simpleByte() {
        byte _byte = 42;
        var bb = ByteBuffer.allocate(Byte.BYTES);
        bb.put(_byte);

        var byteReader = BaseReader.byteReader();
        var state = byteReader.process(bb);
        var result = byteReader.get();

        assertEquals(Reader.ProcessStatus.DONE, state);
        assertEquals(result, _byte);
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void simpleLong() {
        long _long = 42;
        var bb = ByteBuffer.allocate(Long.BYTES);
        bb.putLong(_long);

        var longReader = BaseReader.longReader();
        var state = longReader.process(bb);
        var result = longReader.get();

        assertEquals(Reader.ProcessStatus.DONE, state);
        assertEquals(result, _long);
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void reset() {
        var string = "\u20ACa\u20AC";
        var string2 = "\u20ACa\u20ACabcd";
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode(string);
        var bytes2 = StandardCharsets.UTF_8.encode(string2);
        bb.putInt(bytes.remaining()).put(bytes).putInt(bytes2.remaining()).put(bytes2);
        StringReader sr = new StringReader();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(string, sr.get());
        assertEquals(15, bb.position());
        assertEquals(bb.capacity(), bb.limit());
        sr.reset();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(string2, sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void smallBuffer() {
        var string = "\u20ACa\u20AC";
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode(string);
        bb.putInt(bytes.remaining()).put(bytes).flip();
        var bbSmall = ByteBuffer.allocate(2);
        var sr = new StringReader();
        while (bb.hasRemaining()) {
            while (bb.hasRemaining() && bbSmall.hasRemaining()) {
                bbSmall.put(bb.get());
            }
            if (bb.hasRemaining()) {
                assertEquals(Reader.ProcessStatus.REFILL, sr.process(bbSmall));
            } else {
                assertEquals(Reader.ProcessStatus.DONE, sr.process(bbSmall));
            }
        }
        assertEquals(string, sr.get());
    }

    @Test
    public void errorGet() {
        var sr = new StringReader();
        assertThrows(IllegalStateException.class, sr::get);
    }

    @Test
    public void errorNeg() {
        var sr = new StringReader();
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode("aaaaa");
        bb.putInt(-1).put(bytes);
        assertEquals(Reader.ProcessStatus.ERROR, sr.process(bb));
    }

    @Test
    public void internalBufferMustBeMadeBigger() {
        var sr = new StringReader(StandardCharsets.UTF_8, 16);
        var bb = ByteBuffer.allocate(1024);
        var string = "abcabcabcabcabcabcabcabcabcabcabc";
        var bytes = StandardCharsets.UTF_8.encode(string);
        bb.putInt(bytes.remaining()).put(bytes);
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(string, sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }
}