package fr.uge.ugegreed.utils;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Class providing methods to convert Object to ByteBuffer in write mode
 */
public class TypeToByteWriter {

    /**
     * Return a byteBuffer containing the String encoded in UTF-8
     * prefixed by its byte sized
     * @param input String to write
     * @return ByteBuffer READ mode
     */
    public static ByteBuffer getString(String input, Charset cs) {
        ByteBuffer translatedInput = cs.encode(input);
        return ByteBuffer.allocate(Integer.BYTES + translatedInput.remaining()).putInt(translatedInput.remaining()).put(translatedInput).flip();
    }

    public static ByteBuffer getString(String input) {
        return getString(input, StandardCharsets.UTF_8);
    }

    /**
     * Return a byteBuffer containing the ip (ipv4) and port
     * @param input Address to write
     * @return ByteBuffer READ mode
     */
    public static ByteBuffer getHost(InetSocketAddress input) {
        return ByteBuffer.allocate(6).put(input.getAddress().getAddress()).putShort((short) input.getPort()).flip();
    }
}
