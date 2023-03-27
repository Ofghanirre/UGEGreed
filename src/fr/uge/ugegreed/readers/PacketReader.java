package fr.uge.ugegreed.readers;

import fr.uge.ugegreed.packets.OkDiscPacket;
import fr.uge.ugegreed.packets.Packet;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Reader for complete packets, including initial byte
 */
public class PacketReader implements Reader<Packet> {

  private enum State {
    DONE, WAITING_BYTE, WAITING_PACKET, ERROR
  }

  private State state = State.WAITING_BYTE;
  private Packet.PacketCode packetType;
  private Packet value;

  private Map<Packet.PacketCode, Reader<? extends Packet>> readers = Map.ofEntries(
      Map.entry(Packet.PacketCode.INIT, BasePacketReader.initPacketReader()),
      Map.entry(Packet.PacketCode.UPDT, BasePacketReader.updtPacketReader()),
      Map.entry(Packet.PacketCode.REQ, BasePacketReader.reqPacketReader()),
      Map.entry(Packet.PacketCode.ACC, BasePacketReader.accPacketReader()),
      Map.entry(Packet.PacketCode.REF, BasePacketReader.refPacketReader()),
      Map.entry(Packet.PacketCode.ANS, BasePacketReader.ansPacketReader()),
      Map.entry(Packet.PacketCode.REDI, BasePacketReader.rediPacketReader()),
      Map.entry(Packet.PacketCode.DISC, new DiscPacketReader())
  );

  private Reader<Byte> byteReader = BaseReader.byteReader();

  @Override
  public ProcessStatus process(ByteBuffer byteBuffer) {
    if (state == State.DONE || state == State.ERROR) {
      throw new IllegalStateException();
    }
   while (state != State.DONE && state != State.ERROR) {
     switch (state) {
       case WAITING_BYTE -> {
         var result = byteReader.process(byteBuffer);
         if (result == ProcessStatus.REFILL) { return ProcessStatus.REFILL; }
         if (result == ProcessStatus.ERROR) {
           state = State.ERROR;
           return ProcessStatus.ERROR;
         }
         var packetTypeOpt = Packet.PacketCode.getPacketCodeFromByte(byteReader.get());
         if (packetTypeOpt.isEmpty()) {
           state = State.ERROR;
           return ProcessStatus.ERROR;
         }

         packetType = packetTypeOpt.get();
         // OK_DISC packet is a particular case as there is nothing more to read
         if (packetType == Packet.PacketCode.OK_DISC) {
           value = new OkDiscPacket();
           state = State.DONE;
           return ProcessStatus.DONE;
         }
         if (!readers.containsKey(packetType)) { throw new AssertionError("Missing reader type"); }
         state = State.WAITING_PACKET;
       }

       case WAITING_PACKET -> {
         var result = readers.get(packetType).process(byteBuffer);
         if (result == ProcessStatus.REFILL) { return ProcessStatus.REFILL; }
         if (result == ProcessStatus.ERROR) {
           state = State.ERROR;
           return ProcessStatus.ERROR;
         }

         value = readers.get(packetType).get();
         state = State.DONE;
       }
     }
   }
   return ProcessStatus.DONE;
  }

  @Override
  public Packet get() {
    if (state != State.DONE) {
      throw new IllegalStateException();
    }
    return value;
  }

  @Override
  public void reset() {
    byteReader.reset();
    readers.get(packetType).reset();
    state = State.WAITING_BYTE;
  }

  public static void main(String[] args) {
//    var buffer = ByteBuffer.allocate(1 + 8 + 28)
//        .put((byte) 8)
//        .putInt(34)
//        .putInt(2)
//        .putLong(69).put(TypeToByteWriter.getHost(new InetSocketAddress("255.0.0.1", 1)))
//        .putLong(420).put(TypeToByteWriter.getHost(new InetSocketAddress("127.0.0.3", 65535)));

    var url = StandardCharsets.US_ASCII.encode("hello.com");
    var className = StandardCharsets.UTF_8.encode("Main");
    var buffer = ByteBuffer.allocate(1 + Long.BYTES * 3 + Integer.BYTES * 2 +
            url.remaining() + className.remaining())
        .put((byte) 3)
        .putLong(1)
        .putInt(url.remaining())
        .put(url)
        .putInt(className.remaining())
        .put(className)
        .putLong(1)
        .putLong(1337);

    var reader = new PacketReader();
    var result = reader.process(buffer);
    System.out.println(reader.get());
  }
}
