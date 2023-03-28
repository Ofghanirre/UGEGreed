package fr.uge.ugegreed.tests.packets;

import fr.uge.ugegreed.packets.InitPacket;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InitPacketTest {
  @Test
  public void correctToBuffer() {
    var packet = new InitPacket(0);
    var buffer = ByteBuffer.allocate(1024).put((byte) 1).putInt(0).flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void checkPreconditions() {
    assertThrows(IllegalArgumentException.class, () -> new InitPacket(-1));
  }

  @Test
  public void correctToString() {
    var packet = new InitPacket(42);
    assertEquals("INIT packet(potential: 42)", packet.toString());
  }
}
