package fr.uge.ugegreed.readers;

import java.nio.ByteBuffer;
import java.util.function.Function;

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
    public BaseReader(int bufferSize, Function<ByteBuffer, T> extractor) {
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
}