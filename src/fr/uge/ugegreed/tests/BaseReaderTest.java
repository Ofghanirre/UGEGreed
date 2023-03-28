package fr.uge.ugegreed.tests;

import fr.uge.ugegreed.readers.BaseReader;
import fr.uge.ugegreed.readers.Reader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.IntStream;

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
        int integer1 = 42;
        int integer2 = 77;
        var bb = ByteBuffer.allocate(Integer.BYTES * 2);
        bb.putInt(integer1).putInt(integer2);

        var intReader = BaseReader.intReader();
        var state1 = intReader.process(bb);
        var result1 = intReader.get();

        assertEquals(Reader.ProcessStatus.DONE, state1);
        assertEquals(integer1, result1);
        assertEquals(Integer.BYTES, bb.position());
        assertEquals(bb.capacity(), bb.limit());
        intReader.reset();

        var state2 = intReader.process(bb);
        var result2 = intReader.get();
        assertEquals(Reader.ProcessStatus.DONE, state2);
        assertEquals(integer2, result2);
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void largeBuffer() {
        List<Integer> integers = IntStream.range(0, 1000).boxed().toList();
        var bb = ByteBuffer.allocate(Integer.BYTES * integers.size());
        integers.forEach(bb::putInt);

        var intReader = BaseReader.intReader();
        integers.forEach(integer -> {
            var state = intReader.process(bb);
            var result = intReader.get();
            assertEquals(Reader.ProcessStatus.DONE, state);
            assertEquals(result, integer);
            intReader.reset();
        });

        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void errorGet() {
        var ir = BaseReader.intReader();
        assertThrows(IllegalStateException.class, ir::get);
    }
}