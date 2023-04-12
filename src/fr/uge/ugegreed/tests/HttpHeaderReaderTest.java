package fr.uge.ugegreed.tests;

import fr.uge.ugegreed.http.HttpException;
import fr.uge.ugegreed.http.HttpHeader;
import fr.uge.ugegreed.readers.HttpHeaderReader;
import fr.uge.ugegreed.readers.Reader;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpHeaderReaderTest {
    private final Charset cs = StandardCharsets.US_ASCII;

    @Test
    public void simple() throws HttpException {
        var string =
                "HTTP/1.1 200 OK\r\n"+
                "Content-Type: text/html\r\n" +
                "Content-Length: 1234\r\n" +
                "\r\n";
        var result = HttpHeader.create("HTTP/1.1 200 OK", new HashMap<>() {{
            put("content-length", "1234");
            put("content-type", "text/html");
        }});
        var bb = cs.encode(string).compact();
        HttpHeaderReader sr = new HttpHeaderReader();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(result, sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void simpleWithData() throws HttpException {
        var string =
                "HTTP/1.1 200 OK\r\n"+
                        "Content-Type: text/html\r\n" +
                        "Content-Length: 1234\r\n" +
                        "\r\n" +
                        "Test Data\r\n";
        var result = HttpHeader.create("HTTP/1.1 200 OK", new HashMap<>() {{
            put("content-length", "1234");
            put("content-type", "text/html");
        }});
        var bb = cs.encode(string).compact();
        HttpHeaderReader sr = new HttpHeaderReader();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(result, sr.get());
        assertEquals(11, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void simpleNoCRLFAtFirstThenCRLF() throws HttpException {
        var string1 =
                "HTTP/1.1 200 OK\r\n"+
                        "Content-Type: text/html\r\n";
        var result = HttpHeader.create("HTTP/1.1 200 OK", new HashMap<>() {{
            put("content-length", "1234");
            put("content-type", "text/html");
        }});
        var bb = cs.encode(string1).compact();
        HttpHeaderReader sr = new HttpHeaderReader();
        assertEquals(Reader.ProcessStatus.REFILL, sr.process(bb));
        assertThrows(IllegalStateException.class, sr::get);
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());

        var string2 = "Content-Length: 1234\r\n\r\n";
        bb = cs.encode(string2).compact();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(result, sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void reset() throws HttpException {
        var string =
                "HTTP/1.1 200 OK\r\n"+
                        "Content-Type: text/html\r\n" +
                        "Content-Length: 1234\r\n" +
                        "\r\n" +
                "HTTP/1.1 400 BAD REQUEST\r\n"+
                        "Content-Type: text/html\r\n" +
                        "Content-Length: 42690\r\n" +
                        "\r\n";
        var result1 = HttpHeader.create("HTTP/1.1 200 OK", new HashMap<>() {{
            put("content-length", "1234");
            put("content-type", "text/html");
        }});
        var result2 = HttpHeader.create("HTTP/1.1 400 BAD REQUEST", new HashMap<>() {{
            put("content-length", "42690");
            put("content-type", "text/html");
        }});

        HttpHeaderReader sr = new HttpHeaderReader();

        var bb = cs.encode(string).compact();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(result1, sr.get());
        assertEquals(76, bb.position());
        assertEquals(bb.capacity(), bb.limit());

        sr.reset();
        assertEquals(Reader.ProcessStatus.DONE, sr.process(bb));
        assertEquals(result2, sr.get());
        assertEquals(0, bb.position());
        assertEquals(bb.capacity(), bb.limit());
    }

    @Test
    public void errorGet() {
        var sr = new HttpHeaderReader();
        assertThrows(IllegalStateException.class, sr::get);
    }
}
