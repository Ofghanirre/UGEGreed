package fr.uge.ugegreed.tests.packets;

import fr.uge.ugegreed.packets.ReqPacket;
import fr.uge.ugegreed.utils.TypeToByteWriter;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReqPacketTest {
  @Test
  public void correctToBuffer() {
    var packet = new ReqPacket(1337, "www.jars.com/jar1", "HelloWorld",
        42, 420);
    var buffer = ByteBuffer.allocate(1024)
        .put((byte) 3)
        .putLong(1337)
        .put(TypeToByteWriter.getString("www.jars.com/jar1", StandardCharsets.US_ASCII))
        .put(TypeToByteWriter.getString("HelloWorld"))
        .putLong(42)
        .putLong(420)
        .flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void checkPreconditions() {
    assertThrows(IllegalArgumentException.class, () ->
        new ReqPacket(-1, "abc", "abc", 0, 1));
    assertThrows(NullPointerException.class, () ->
        new ReqPacket(0, null, "abc", 0, 1));
    assertThrows(NullPointerException.class, () ->
        new ReqPacket(0, "abc", null, 0, 1));
    assertThrows(IllegalArgumentException.class, () ->
        new ReqPacket(0, "abc", "abc", 25, 12));
    assertThrows(IllegalArgumentException.class, () ->
        new ReqPacket(0, "", "abc", 0, 1));
    assertThrows(IllegalArgumentException.class, () ->
        new ReqPacket(0, "abc", "", 0, 1));
  }

  @Test
  public void correctToString() {
    var packet = new ReqPacket(1337, "www.jars.com/jar1", "HelloWorld",
        42, 420);
    assertEquals("REQ packet(job_id: 1337, jar_URL: www.jars.com/jar1, class_name: HelloWorld, range_start: 42, range_end: 420)", packet.toString());
  }
}
