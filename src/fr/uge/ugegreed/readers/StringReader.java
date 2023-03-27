package fr.uge.ugegreed.readers;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class StringReader implements Reader<String> {
	private enum State {
    DONE, WAITING_SIZE, WAITING_STRING, ERROR
	}
	
	private static final int BUFFER_SIZE = 1024;
	private final Charset charset;
	
	private State state = State.WAITING_SIZE;
	private final ByteBuffer internalBuffer = ByteBuffer.allocate(BUFFER_SIZE);
	private String value;
	
	public StringReader(Charset charset) {
		this.charset = Objects.requireNonNull(charset);
		internalBuffer.limit(Integer.BYTES);
	}

	public StringReader() {
		this(StandardCharsets.UTF_8);
	}

	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
		}
		for (;;) {
			buffer.flip();
			if (buffer.remaining() <= internalBuffer.remaining()) {
	      internalBuffer.put(buffer);
	    } else {
	        var oldLimit = buffer.limit();
	        buffer.limit(internalBuffer.remaining());
	        internalBuffer.put(buffer);
	        buffer.limit(oldLimit);
	    }
			buffer.compact();
			
			if (internalBuffer.hasRemaining()) {
        return ProcessStatus.REFILL;
			}
			
			switch (state) {
			case WAITING_SIZE -> {
				internalBuffer.flip();
				var size = internalBuffer.getInt();
				if (size > BUFFER_SIZE || size < 0) { 
					state = State.ERROR;
					return ProcessStatus.ERROR;
				}
				internalBuffer.clear();
				internalBuffer.limit(size);
				state = State.WAITING_STRING;
			}
			case WAITING_STRING -> {
				internalBuffer.flip();
				value = charset.decode(internalBuffer).toString();
				state = State.DONE;
				return ProcessStatus.DONE;
			}
			default -> throw new AssertionError();
			}
		}		
	}

	@Override
	public String get() {
		if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return value;
	}

	@Override
	public void reset() {
		state = State.WAITING_SIZE;
    internalBuffer.clear();
    internalBuffer.limit(Integer.BYTES);
	}
}
