package fr.uge.ugegreed.tests.packets;

import fr.uge.ugegreed.packets.DiscPacket;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DiscPacketTest {
  @Test
  public void correctToBufferWithJobs() {
    var packet = new DiscPacket(1, 2, new DiscPacket.InnerDiscPacket[]{
        new DiscPacket.InnerDiscPacket(12, 7777),
        new DiscPacket.InnerDiscPacket(33, 7222)
    });
    var buffer = ByteBuffer.allocate(1024)
        .put((byte)8)
        .putInt(1)
        .putInt(2)
        .putLong(12)
        .putInt(7777)
        .putLong(33)
        .putInt(7222)
        .flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void correctToBufferNoJobs() {
    var packet = new DiscPacket(0, 0, new DiscPacket.InnerDiscPacket[0]);
    var buffer = ByteBuffer.allocate(1024)
        .put((byte)8)
        .putInt(0)
        .putInt(0)
        .flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void checkPreconditions() {
    assertThrows(NullPointerException.class, () ->
        new DiscPacket(0, 0, null));
    assertThrows(IllegalArgumentException.class, () ->
        new DiscPacket(-1, 0, new DiscPacket.InnerDiscPacket[0]));
    assertThrows(IllegalArgumentException.class, () ->
        new DiscPacket(0, -1, new DiscPacket.InnerDiscPacket[0]));
    assertThrows(IllegalArgumentException.class, () ->
        new DiscPacket(1, 1,
            new DiscPacket.InnerDiscPacket[]{new DiscPacket.InnerDiscPacket(-1, 7777)}));
    assertThrows(IllegalArgumentException.class, () ->
        new DiscPacket(1, 2,
            new DiscPacket.InnerDiscPacket[]{new DiscPacket.InnerDiscPacket(12,
                7777)}));
  }

  @Test
  public void correctToString() {
    var packet = new DiscPacket(1, 1, new DiscPacket.InnerDiscPacket[]{
        new DiscPacket.InnerDiscPacket(12, 7777)});
    assertEquals("DISC packet(nb_reco: 1, nb_jobs: 1, jobs: [job_id: 12, new_upstream: 7777])", packet.toString());
  }
}
