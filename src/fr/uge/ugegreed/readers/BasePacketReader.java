package fr.uge.ugegreed.readers;

import fr.uge.ugegreed.packets.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Contains factories for readers for most packet types
 * These do NOT read the first initial byte, as it's considered to have been read already
 * @param <T> packet type to be read
 */
public class BasePacketReader<T> implements Reader<T> {
  private enum State {
    DONE, WAITING, ERROR
  }

  private State state = State.WAITING;
  private int step;
  private final int numberOfSteps;
  private final List<Reader<?>> readers;
  private final Function<List<Reader<?>>, Optional<T>> extractor;
  private T value;

  private BasePacketReader(List<Reader<?>> readers, Function<List<Reader<?>>, Optional<T>> extractor) {
    this.readers = readers;
    numberOfSteps = readers.size();
    this.extractor = extractor;
  }

  @Override
  public ProcessStatus process(ByteBuffer byteBuffer) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }

    while (step < numberOfSteps) {
      var readerState = readers.get(step).process(byteBuffer);
      switch (readerState) {
        case REFILL -> {
          return ProcessStatus.REFILL;
        }
        case ERROR -> {
          state = State.ERROR;
          return ProcessStatus.ERROR;
        }
        case DONE -> step++;
      }
    }

    var result = extractor.apply(readers);
    if (result.isEmpty()) {
      state = State.ERROR;
      return ProcessStatus.ERROR;
    }
    value = result.get();
    state = State.DONE;
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
    step = 0;
    state = State.WAITING;
    readers.forEach(Reader::reset);
  }

  // All the casts are safe as only those factories can initialize instances of this class, so we know exactly
  // what types each get() call will return
  @SuppressWarnings("unchecked")
  public static Reader<InitPacket> initPacketReader() {
    return new BasePacketReader<>(List.of(BaseReader.intReader()), readers -> {
      int value = ((Reader<Integer>) readers.get(0)).get();
      if (value < 0) {
        return Optional.empty();
      }
      return Optional.of(new InitPacket(value));
    });
  }

  @SuppressWarnings("unchecked")
  public static Reader<UpdtPacket> updtPacketReader() {
    return new BasePacketReader<>(List.of(BaseReader.intReader()), readers -> {
      int value = ((Reader<Integer>) readers.get(0)).get();
      if (value < 0) {
        return Optional.empty();
      }
      return Optional.of(new UpdtPacket(value));
    });
  }

  @SuppressWarnings("unchecked")
  public static Reader<ReqPacket> reqPacketReader() {
    return new BasePacketReader<>(List.of(BaseReader.longReader(), new StringReader(StandardCharsets.US_ASCII),
      new StringReader(), BaseReader.longReader(), BaseReader.longReader()),
      readers -> {
        var job_id = ((Reader<Long>) readers.get(0)).get();
        var url = ((Reader<String>) readers.get(1)).get();
        var className = ((Reader<String>) readers.get(2)).get();
        var rangeStart = ((Reader<Long>) readers.get(3)).get();
        var rangeEnd = ((Reader<Long>) readers.get(4)).get();

        try {
          return Optional.of(new ReqPacket(job_id, url, className, rangeStart, rangeEnd));
        } catch (IllegalArgumentException e) {
          return Optional.empty();
        }
      });
  }

  @SuppressWarnings("unchecked")
  public static Reader<AccPacket> accPacketReader() {
    return new BasePacketReader<>(List.of(BaseReader.longReader(), BaseReader.longReader(), BaseReader.longReader()),
        readers -> {
          var job_id = ((Reader<Long>) readers.get(0)).get();
          var rangeStart = ((Reader<Long>) readers.get(1)).get();
          var rangeEnd = ((Reader<Long>) readers.get(2)).get();

          try {
            return Optional.of(new AccPacket(job_id, rangeStart, rangeEnd));
          } catch (IllegalArgumentException e) {
            return Optional.empty();
          }
        });
  }

  @SuppressWarnings("unchecked")
  public static Reader<RefPacket> refPacketReader() {
    return new BasePacketReader<>(List.of(BaseReader.longReader(), BaseReader.longReader(), BaseReader.longReader()),
        readers -> {
          var job_id = ((Reader<Long>) readers.get(0)).get();
          var rangeStart = ((Reader<Long>) readers.get(1)).get();
          var rangeEnd = ((Reader<Long>) readers.get(2)).get();

          try {
            return Optional.of(new RefPacket(job_id, rangeStart, rangeEnd));
          } catch (IllegalArgumentException e) {
            return Optional.empty();
          }
        });
  }

  @SuppressWarnings("unchecked")
  public static Reader<AnsPacket> ansPacketReader() {
    return new BasePacketReader<>(List.of(BaseReader.longReader(), BaseReader.longReader(), new StringReader()),
      readers -> {
        var job_id = ((Reader<Long>) readers.get(0)).get();
        var number = ((Reader<Long>) readers.get(1)).get();
        var result = ((Reader<String>) readers.get(2)).get();

        try {
          return Optional.of(new AnsPacket(job_id, number, result));
        } catch (IllegalArgumentException e) {
          return Optional.empty();
        }
      });
  }

  @SuppressWarnings("unchecked")
  public static Reader<RediPacket> rediPacketReader() {
    return new BasePacketReader<>(List.of(BaseReader.hostReader()),
      readers -> {
        var host = ((Reader<InetSocketAddress>) readers.get(0)).get();

        try {
          return Optional.of(new RediPacket(host));
        } catch (IllegalArgumentException e) {
          return Optional.empty();
        }
      });
  }

  public static void main(String[] args) {
//    var buffer = ByteBuffer.allocate(5).putInt(69);
//    var reader = initPacketReader();
//    reader.process(buffer);
//    System.out.println(reader.get());

    var url = StandardCharsets.US_ASCII.encode("hello.com");
    var className = StandardCharsets.UTF_8.encode("Main");
    var buffer = ByteBuffer.allocate(Long.BYTES * 3 + Integer.BYTES * 2 +
            url.remaining() + className.remaining())
        .putLong(-1)
        .putInt(url.remaining())
        .put(url)
        .putInt(className.remaining())
        .put(className)
        .putLong(1)
        .putLong(1337);

    var reader = reqPacketReader();
    var result = reader.process(buffer);
    if (result == ProcessStatus.ERROR) {
      System.err.println("Result is error");
      return;
    }
    System.out.println(reader.get());
    reader.reset();

//    var host = new InetSocketAddress("127.0.0.69", 6969);
//    var buffer = TypeToByteWriter.getHost(host).compact();
//    var reader = rediPacketReader();
//    reader.process(buffer);
//    System.out.println(reader.get());
//    reader.reset();

  }
}
