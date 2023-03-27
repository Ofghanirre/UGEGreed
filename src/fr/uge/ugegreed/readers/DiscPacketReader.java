package fr.uge.ugegreed.readers;

import fr.uge.ugegreed.packets.DiscPacket;
import fr.uge.ugegreed.utils.TypeToByteWriter;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DiscPacketReader implements Reader<DiscPacket> {

  private enum State {
    DONE, WAITING_NB_RECO, WAITING_NB_JOBS, WAITING_HOSTS, ERROR
  }

  private State state = State.WAITING_NB_RECO;
  private int step;
  private int numberOfReconnections;
  private int numberOfJobs;
  private DiscPacket.InnerDiscPacket[] innerDiscPackets;

  private final Reader<Integer> intReader = BaseReader.intReader();
  private final Reader<DiscPacket.InnerDiscPacket> innerDiscReader = BasePacketReader.innerDiscPacketReader();


  @Override
  public ProcessStatus process(ByteBuffer byteBuffer) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }

    while (state != State.DONE && state != State.ERROR) {
      switch (state) {
        case WAITING_NB_RECO -> {
          var result = intReader.process(byteBuffer);
          if (result == ProcessStatus.REFILL) { return ProcessStatus.REFILL; }
          if (result == ProcessStatus.ERROR) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
          }
          numberOfReconnections = intReader.get();
          if (numberOfReconnections < 0) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
          }
          intReader.reset();
          state = State.WAITING_NB_JOBS;
        }

        case WAITING_NB_JOBS -> {
          var result = intReader.process(byteBuffer);
          if (result == ProcessStatus.REFILL) { return ProcessStatus.REFILL; }
          if (result == ProcessStatus.ERROR) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
          }
          numberOfJobs = intReader.get();
          if (numberOfJobs < 0) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
          }
          innerDiscPackets = new DiscPacket.InnerDiscPacket[numberOfJobs];
          state = State.WAITING_HOSTS;
        }

        case WAITING_HOSTS -> {
          var result = innerDiscReader.process(byteBuffer);
          if (result == ProcessStatus.REFILL) { return ProcessStatus.REFILL; }
          if (result == ProcessStatus.ERROR) {
            state = State.ERROR;
            return ProcessStatus.ERROR;
          }
          innerDiscPackets[step] = innerDiscReader.get();
          innerDiscReader.reset();
          step++;
          if (step >= numberOfJobs) { state = State.DONE; }
        }
      }
    }

    return ProcessStatus.DONE;
  }

  @Override
  public DiscPacket get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return new DiscPacket(numberOfReconnections, numberOfJobs, innerDiscPackets);
  }

  @Override
  public void reset() {
    step = 0;
    innerDiscPackets = null;
    intReader.reset();
    innerDiscReader.reset();
    state = State.WAITING_NB_RECO;
  }

  public static void main(String[] args) {
    var buffer = ByteBuffer.allocate(8 + 28)
        .putInt(34)
        .putInt(2)
        .putLong(69).put(TypeToByteWriter.getHost(new InetSocketAddress("255.0.0.1", 1)))
        .putLong(420).put(TypeToByteWriter.getHost(new InetSocketAddress("127.0.0.3", 65535)));

    var reader = new DiscPacketReader();
    reader.process(buffer);
    System.out.println(reader.get());
  }
}
