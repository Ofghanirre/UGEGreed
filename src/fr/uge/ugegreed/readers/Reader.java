package fr.uge.ugegreed.readers;

import java.nio.ByteBuffer;

/**
 * Interface that represents classes which read data from byte buffers
 * @param <T> type that is to be returned by the reader once it's done
 */
public interface Reader<T> {
    enum ProcessStatus { DONE, REFILL, ERROR };

    /**
     * Processes a byte buffer
     * @param byteBuffer in write mode
     * @return status after processing
     * @throws IllegalStateException if trying to process while status is DONE or ERROR
     */
    ProcessStatus process(ByteBuffer byteBuffer);

    /**
     * Returns the data that was read
     * @return data that was read
     * @throws IllegalStateException if trying to get while status is not DONE
     */
    T get();

    /**
     * Reinitializes reader
     */
    void reset();
}
