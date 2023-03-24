package fr.uge.ugegreed.utils;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Class providing methods to convert Object to ByteBuffer in write mode
 */
public class TypeToByteWriter {
    private static final Charset cs = StandardCharsets.UTF_8;

    /**
     * Return a byteBuffer containing the String encoded in UTF-8
     * prefixed by its byte sized
     * @param input String to write
     * @return ByteBuffer WRITE mode
     */
    public static ByteBuffer getString(String input) {
        ByteBuffer translatedInput = cs.encode(input).flip();
        return ByteBuffer.allocate(Integer.BYTES + translatedInput.capacity()).putInt(translatedInput.capacity()).put(translatedInput);
    }

    /**
     * Return a byteBuffer containing the ip (ipv4) and port
     * @param input Address to write
     * @return ByteBuffer WRITE mode
     */
    public static ByteBuffer getHost(InetSocketAddress input) {
        return ByteBuffer.allocate(6).put(input.getAddress().getAddress()).putInt(input.getPort());
    }
}
