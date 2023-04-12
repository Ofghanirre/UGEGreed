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
    private State state = State.WAITING;
    private final HttpHeaderLineReader lineReader = new HttpHeaderLineReader();

    /**
     * Process a ByteBuffer flow as an HttpResponse and parse its header line per line using the
     * HttpHeaderLineReader
     *
     * @param buffer in write mode
     * @return
     */
    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE) {
            throw new IllegalStateException("Trying to process on an already DONE job, please reset the reader.");
        }
        while (state != State.DONE) {
            var status = lineReader.process(buffer);
            switch (status) {
                case DONE -> {
                    var line = lineReader.get();
                    if (line.equals("")) {
                        state = State.DONE;
                        break;
                    }
                    resultlines.add(line);
                    lineReader.reset();
                }
                case REFILL -> {
                    return ProcessStatus.REFILL;
                }
                case ERROR -> throw new AssertionError();
            }
        }
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
        lineReader.reset();
    }
}
