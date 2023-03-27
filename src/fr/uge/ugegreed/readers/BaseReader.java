package fr.uge.ugegreed.readers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Base class for making basic readers which only need a fixed size buffer and
 * one method to extract the output from the bytebuffer
 * @param <T> type the reader extracts
 */
public class BaseReader<T> implements Reader<T> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private final ByteBuffer internalBuffer;
    private T value;
    private final Function<ByteBuffer, T> extractor;

    /**
     * Creates a new BaseReader
     * @param bufferSize size of the internal buffer that needs to be filled to read an element
     * @param extractor function which reads the element from the filled buffer
     */
    private BaseReader(int bufferSize, Function<ByteBuffer, T> extractor) {
        this.extractor = extractor;
        internalBuffer = ByteBuffer.allocate(bufferSize);
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        buffer.flip();
        try {
            if (buffer.remaining() <= internalBuffer.remaining()) {
                internalBuffer.put(buffer);
            } else {
                var oldLimit = buffer.limit();
                buffer.limit(internalBuffer.remaining());
                internalBuffer.put(buffer);
                buffer.limit(oldLimit);
            }
        } finally {
            buffer.compact();
        }
        if (internalBuffer.hasRemaining()) {
            return ProcessStatus.REFILL;
        }
        state = State.DONE;
        internalBuffer.flip();
        value = extractor.apply(internalBuffer);
        return ProcessStatus.DONE;
    }

    @Override
    public T get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        internalBuffer.clear();
    }

    /**
     * Returns a reader for single bytes
     * @return reader for single bytes
     */
    public static Reader<Byte> byteReader() {
        return new BaseReader<>(Byte.BYTES, ByteBuffer::get);
    }

    /**
     * Returns a reader for integers
     * @return reader for integers
     */
    public static Reader<Integer> intReader() {
        return new BaseReader<>(Integer.BYTES, ByteBuffer::getInt);
    }

    /**
     * Returns a reader for longs
     * @return reader for longs
     */
    public static Reader<Long> longReader() {
        return new BaseReader<>(Long.BYTES, ByteBuffer::getLong);
    }

    /**
     * Returns a reader for hosts (IPv4 + port)
     * @return reader for hosts
     */
    public static Reader<InetSocketAddress> hostReader() {
        return new BaseReader<>(6, byteBuffer -> {
            var ip = new byte[4];
            IntStream.range(0, 4).forEach(i -> ip[i] = byteBuffer.get());
            var port = Short.toUnsignedInt(byteBuffer.getShort());
            try {
                return new InetSocketAddress(InetAddress.getByAddress(ip), port);
            } catch (UnknownHostException e) {
                // ip array is necessarily of the correct size
                throw new AssertionError();
            }
        });
    }

    public static void main(String[] args) {
//    var buffer = ByteBuffer.allocate(Integer.BYTES).putInt(2_000_000_000);
//    var reader = intReader();
//    reader.process(buffer);
//    System.out.println(reader.get());

//    var buffer = ByteBuffer.allocate(Integer.BYTES).put((byte)69);
//    var reader = byteReader();
//    reader.process(buffer);
//    System.out.println(reader.get());

        byte[] ip = {127, 0, 0, 1};
        var buffer = ByteBuffer.allocate(6).put(ip).putShort((short) 7777);
        var reader = hostReader();
        reader.process(buffer);
        var address = reader.get();
        System.out.println(address.getAddress() + " " + address.getPort());

    }
}