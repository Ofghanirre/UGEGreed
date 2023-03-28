package fr.uge.ugegreed.tests.packets;

import fr.uge.ugegreed.packets.AnsPacket;
import fr.uge.ugegreed.utils.TypeToByteWriter;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AnsPacketTest {
  @Test
  public void correctToBuffer() {
    var packet = new AnsPacket(9001, 42, "IT IS OVER NINE THOUSAND!");
    var buffer = ByteBuffer.allocate(1024)
        .put((byte) 6)
        .putLong(9001)
        .putLong(42)
        .put(TypeToByteWriter.getString("IT IS OVER NINE THOUSAND!"))
        .flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void checkPreconditions() {
    assertThrows(IllegalArgumentException.class, () ->
        new AnsPacket(-1, 0, ""));
    assertThrows(NullPointerException.class, () ->
        new AnsPacket(21,25, null));
  }

  @Test
  public void correctToString() {
    var packet = new AnsPacket(1,2, "3 bras de fer chinois");
    assertEquals(packet.toString(), "ANS packet(job_id: 1, number: 2, result: 3 bras de fer chinois)");
  }
}
