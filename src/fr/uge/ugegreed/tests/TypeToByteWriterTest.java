package fr.uge.ugegreed.tests;

import fr.uge.ugegreed.utils.TypeToByteWriter;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeToByteWriterTest {
    @Test
    public void simpleString() {
        var string = "€a€";
        var bb = TypeToByteWriter.getString(string);
        bb.getInt();
        var result = StandardCharsets.UTF_8.decode(bb).toString();
        assertEquals("€a€", result);
    }

    @Test
    public void simpleStringWithCharset() {
        var string = "coucou";
        var bb = TypeToByteWriter.getString(string, StandardCharsets.US_ASCII);
        bb.getInt();
        var result = StandardCharsets.US_ASCII.decode(bb).toString();
        assertEquals("coucou", result);
    }

    @Test
    public void simpleHost() throws UnknownHostException {
        var host = new InetSocketAddress("42.69.00.30", 7777);
        var bb = TypeToByteWriter.getHost(host);

        byte[] addrBytes = new byte[4];
        bb.get(addrBytes);
        InetAddress addr = InetAddress.getByAddress(addrBytes);
        int port = bb.getShort();

        var newHost = new InetSocketAddress(addr, port);
        assertEquals(host, newHost);
    }
}