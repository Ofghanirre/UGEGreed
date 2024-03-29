package fr.uge.ugegreed.tests.packets;

import fr.uge.ugegreed.packets.UpdtPacket;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UpdtPacketTest {
  @Test
  public void correctToBuffer() {
    var packet = new UpdtPacket(0, 7777);
    var buffer = ByteBuffer.allocate(1024).put((byte) 2).putInt(0).putInt(7777).flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void checkPreconditions() {
    assertThrows(IllegalArgumentException.class, () -> new UpdtPacket(-1, 7777));
  }

  @Test
  public void correctToString() {
    var packet = new UpdtPacket(42, 7778);
    assertEquals("UPDT packet(potential: 42, appID: 7778)", packet.toString());
  }
}
