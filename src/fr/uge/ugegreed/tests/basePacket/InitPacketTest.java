package fr.uge.ugegreed.tests.basePacket;

import fr.uge.ugegreed.packets.InitPacket;
import fr.uge.ugegreed.readers.BasePacketReader;
import fr.uge.ugegreed.readers.Reader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InitPacketTest {
    @Test
    public void simpleInitPacket() {
        var packet = new InitPacket(3, 7777);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var reader = BasePacketReader.initPacketReader();

        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void resetInitPacket() {
        var packet = new InitPacket(42, 7777);
        var packet2 = new InitPacket(3333, 7777);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var buffer2 = packet2.toBuffer();
        buffer2.position(buffer2.position() + 1).compact();

        var reader = BasePacketReader.initPacketReader();

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
    public void fragmentedInitPacket() {
        var packet = new InitPacket(3, 7777);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1);

        var oldLimit = buffer.limit();
        buffer.limit(buffer.position() + 2);
        var buffer2 = ByteBuffer.allocate(2).put(buffer);
        buffer.limit(oldLimit).compact();

        var reader = BasePacketReader.initPacketReader();

        assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer2));
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void errorNegInitPacker() {
        var buffer = ByteBuffer.allocate(8).putInt(-2).putInt(7777);
        var reader = BasePacketReader.initPacketReader();

        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    public void errorGetInitPacket() {
        var reader = BasePacketReader.initPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }
}
