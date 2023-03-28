package fr.uge.ugegreed.tests.packets;

import fr.uge.ugegreed.packets.AccPacket;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccPacketTest {
  @Test
  public void correctToBuffer() {
    var packet = new AccPacket(1337, 1, 10);
    var buffer = ByteBuffer.allocate(1024)
        .put((byte) 4)
        .putLong(1337)
        .putLong(1)
        .putLong(10)
        .flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void checkPreconditions() {
    assertThrows(IllegalArgumentException.class, () ->
        new AccPacket(-1, 0, 1));
    assertThrows(IllegalArgumentException.class, () ->
        new AccPacket(21,25, 12));
  }

  @Test
  public void correctToString() {
    var packet = new AccPacket(1337,42, 420);
    assertEquals("ACC packet(job_id: 1337, range_start: 42, range_end: 420)", packet.toString());
  }
}
