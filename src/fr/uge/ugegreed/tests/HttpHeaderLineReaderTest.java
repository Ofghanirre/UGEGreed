package fr.uge.ugegreed.tests;

import fr.uge.ugegreed.readers.HttpHeaderLineReader;
import fr.uge.ugegreed.readers.Reader;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpHeaderLineReaderTest {
    private final Charset cs = StandardCharsets.US_ASCII;

    private String extractCRLF(String string) {
        return string.substring(0, string.length()-2);
    }
    @Test
    public void simple() {
        var string = "Test Header with a simple CRLF at the end \r\n";
        var bb = cs.encode(string).compact();
        HttpHeaderLineReader sr = new HttpHeaderLineReader();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(extractCRLF(string), sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void simpleEmptyLineWithCRLF() {
        var string = "\r\n";
        var bb = cs.encode(string).compact();
        HttpHeaderLineReader sr = new HttpHeaderLineReader();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(extractCRLF(string), sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void simpleNoCRLFAtFirstThenCRLF() {
        var string1 = "Test Header with no simple CRLF at the end!";
        var bb = cs.encode(string1).compact();
        HttpHeaderLineReader sr = new HttpHeaderLineReader();
        assertEquals(Reader.ProcessStatus.REFILL, sr.process(bb));
        assertThrows(IllegalStateException.class, sr::get);
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());

        var string2 = "But oh wait there is finally one \r\n";
        bb = cs.encode(string2).compact();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(string1+extractCRLF(string2), sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void simpleWithOtherSpecialChar() {
        var string = "Test Header with a simple CRLF at the end\nBut filled with other\r\tspecial char\ra \r\r\n";
        var bb = cs.encode(string).compact();
        HttpHeaderLineReader sr = new HttpHeaderLineReader();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(extractCRLF(string), sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void reset() {
        var string1 = "Test1\r\n";
        var string2 = "Test2\r\n";
        var string = string1+string2;

        HttpHeaderLineReader sr = new HttpHeaderLineReader();

        var bb = cs.encode(string).compact();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(extractCRLF(string1), sr.get());
        assertEquals(7, bb.position());
        assertEquals(bb.capacity(), bb.limit());

        sr.reset();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(extractCRLF(string2), sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void errorGet() {
        var sr = new HttpHeaderLineReader();
        assertThrows(IllegalStateException.class, sr::get);
    }
}