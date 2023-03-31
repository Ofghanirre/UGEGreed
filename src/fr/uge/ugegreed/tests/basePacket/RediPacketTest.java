package fr.uge.ugegreed.tests.basePacket;

import fr.uge.ugegreed.packets.RediPacket;
import fr.uge.ugegreed.readers.BasePacketReader;
import fr.uge.ugegreed.readers.Reader;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RediPacketTest {
    @Test
    public void simpleRediPacket() {
        var packet = new RediPacket(new InetSocketAddress("42.69.00.30", 7777));
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var reader = BasePacketReader.rediPacketReader();

        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void resetRediPacket() {
        var packet = new RediPacket(new InetSocketAddress("42.69.00.30", 7777));
        var packet2 = new RediPacket(new InetSocketAddress("42.69.200.30", 6666));
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var buffer2 = packet2.toBuffer();
        buffer2.position(buffer2.position() + 1).compact();

        var reader = BasePacketReader.rediPacketReader();

        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
        reader.reset();
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer2));
        assertEquals(packet2, reader.get());
        assertEquals(0, buffer2.position());
        assertEquals(buffer2.capacity(), buffer2.limit());
    }

    @Test
    public void fragmentedRediPacket() {
        var packet = new RediPacket(new InetSocketAddress("42.69.00.30", 7777));
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1);

        var oldLimit = buffer.limit();
        buffer.limit(buffer.position() + 2);
        var buffer2 = ByteBuffer.allocate(2).put(buffer);
        buffer.limit(oldLimit).compact();

        var reader = BasePacketReader.rediPacketReader();

        assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer2));
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void errorGetRediPacket() {
        var reader = BasePacketReader.rediPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }
}
