package fr.uge.ugegreed.readers;

import fr.uge.ugegreed.packets.InitPacket;

import java.nio.ByteBuffer;

/**
 * Reads INIT packets (minus the first byte corresponding to the packet code)
 */
public class InitPacketReader implements Reader<InitPacket> {
  private enum State {
    DONE, WAITING, ERROR
  }

  private State state = State.WAITING;
  private final Reader<Integer> intReader = BaseReaders.intReader();
  private int potential;

  @Override
  public ProcessStatus process(ByteBuffer byteBuffer) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }

    var res = intReader.process(byteBuffer);

    if (res == ProcessStatus.REFILL) { return res; }
    if (res == ProcessStatus.ERROR) {
      state = State.ERROR;
      return res;
    }

    potential = intReader.get();
    if (potential < 0) {
      state = State.ERROR;
      return ProcessStatus.ERROR;
    }
    state = State.DONE;

    return ProcessStatus.DONE;
  }

  @Override
  public InitPacket get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return new InitPacket(potential);
  }

  @Override
  public void reset() {
    state = State.WAITING;
    intReader.reset();
  }

  public static void main(String[] args) {
    var buffer = ByteBuffer.allocate(5).putInt(12);
    var reader = new InitPacketReader();
    reader.process(buffer);
    System.out.println(reader.get());
  }
}
