package fr.uge.ugegreed.tests.basePacket;

import fr.uge.ugegreed.packets.DiscPacket;
import fr.uge.ugegreed.readers.BasePacketReader;
import fr.uge.ugegreed.readers.Reader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InnerDiscPacketTest {
    @Test
    public void simpleInnerDiscPacket() {
        var packet = new DiscPacket.InnerDiscPacket(1, 7777);

        var buffer = packet.toBuffer();
        buffer.compact();
        var reader = BasePacketReader.innerDiscPacketReader();

        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void resetInnerDiscPacket() {
        var packet = new DiscPacket.InnerDiscPacket(1, 7777);
        var packet2 = new DiscPacket.InnerDiscPacket(2,6666);
        var buffer = packet.toBuffer();
        buffer.compact();
        var buffer2 = packet2.toBuffer();
        buffer2.compact();

        var reader = BasePacketReader.innerDiscPacketReader();

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
    public void fragmentedInnerDiscPacket() {
        var packet = new DiscPacket.InnerDiscPacket(1, 7777);
        var buffer = packet.toBuffer();

        var oldLimit = buffer.limit();
        buffer.limit(buffer.position() + 2);
        var buffer2 = ByteBuffer.allocate(2).put(buffer);
        buffer.limit(oldLimit).compact();

        var reader = BasePacketReader.innerDiscPacketReader();

        assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer2));
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void errorNegInnerDiscPacker() {
        long job_id = -1;
        var address = 7777;
        var buffer = ByteBuffer.allocate(Byte.BYTES + Long.BYTES + Integer.BYTES);
        buffer.putLong(job_id).putInt(address);

        var reader = BasePacketReader.innerDiscPacketReader();
        // JOB_ID INVALID
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    public void errorGetInnerDiscPacket() {
        var reader = BasePacketReader.innerDiscPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }
}
