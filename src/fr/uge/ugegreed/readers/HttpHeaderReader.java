package fr.uge.ugegreed.readers;

import fr.uge.ugegreed.http.HttpException;
import fr.uge.ugegreed.http.HttpHeader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HttpHeaderReader implements Reader<HttpHeader> {
    private enum State {
        DONE, WAITING
    }
    private final List<String> resultlines = new ArrayList<>();
    private boolean gotEndLine = false;
    private State state = State.WAITING;
    private final HttpHeaderLineReader lineReader = new HttpHeaderLineReader();

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE) {
            throw new IllegalStateException("Trying to process on an already DONE job, please reset the reader.");
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            if (lineReader.process(buffer) == ProcessStatus.DONE) {
                var line = lineReader.get();
                if (line.equals("")) {
                    gotEndLine = true;
                    break;
                }
                resultlines.add(line);
            }
        }
        buffer.compact();

        if (!gotEndLine) return ProcessStatus.REFILL;
        state = State.DONE;
        return ProcessStatus.DONE;
    }

    @Override
    public HttpHeader get() {
        if (state != State.DONE) {
            throw new IllegalStateException("Can not get as the result as the processState isn't DONE");
        }
        var fields = new HashMap<String, String>();
        for (int i = 1; i < resultlines.size(); i++) {
            var line = resultlines.get(i);
            var separator = line.indexOf(":");
            var key = line.substring(0, separator);
            var value = line.substring(separator+2);
            fields.merge(key, value, (v1, v2) -> v1 + ";" + v2);
        }
        try {
            return HttpHeader.create(resultlines.get(0), fields);
        } catch (HttpException e) {
            // TODO HANDLE HTTP EXCEPTION
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reset() {
        state = State.WAITING;
        resultlines.clear();
        gotEndLine = false;
    }
}
