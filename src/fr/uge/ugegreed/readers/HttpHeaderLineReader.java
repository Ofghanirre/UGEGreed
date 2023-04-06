package fr.uge.ugegreed.readers;

import java.nio.ByteBuffer;

/**
 * Reader for a HTTPHeader Line, a sequence of char suffixed with a CRLF : '\r\n'
 */
public class HttpHeaderLineReader implements Reader<String> {
    private enum State {
        DONE, WAITING
    }
    private State state = State.WAITING;
    private boolean gotCRLF = false;
    private final StringBuilder resultLine = new StringBuilder();

    /**
     * Read a HTTPHeader line from a buffer, if the buffer doesn't fully contains the line a ProcessStatus.REFILL state
     * will be returned, the process method will have to be called again until a ProcessStatus.DONE is returned.
     * @param buffer in write mode
     * @return ProcessStatus.DONE if the line is read, ProcessStatus.REFILL if not
     */
    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE) {
            throw new IllegalStateException("Trying to process on an already DONE job, please reset the reader.");
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            var c = (char) buffer.get();
            if (gotCRLF) {
                if (c == '\n') { break; }
                else if (c == '\r') { resultLine.append(c); }
                else {
                    resultLine.append('\r');
                    resultLine.append(c);
                    gotCRLF = false;
                }
            } else {
                if (c == '\r') { gotCRLF = true; }
                else { resultLine.append(c); }
            }
        }

        if (!gotCRLF) {
            return ProcessStatus.REFILL;
        }
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public String get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Can not get as the result as the processState isn't DONE");
        }
        return resultLine.toString();
    }

    @Override
    public void reset() {
        state = State.WAITING;
        resultLine.setLength(0);
    }
}
