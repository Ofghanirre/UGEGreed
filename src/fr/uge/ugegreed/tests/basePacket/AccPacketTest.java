package fr.uge.ugegreed.tests.basePacket;

import fr.uge.ugegreed.packets.AccPacket;
import fr.uge.ugegreed.readers.BasePacketReader;
import fr.uge.ugegreed.readers.Reader;
import fr.uge.ugegreed.utils.TypeToByteWriter;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccPacketTest {
    @Test
    public void simpleAccPacket() {
        var packet = new AccPacket(1,  0, 42);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var reader = BasePacketReader.accPacketReader();

        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void resetAccPacket() {
        var packet = new AccPacket(1,  0, 42);
        var packet2 = new AccPacket(2,  200, 242);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var buffer2 = packet2.toBuffer();
        buffer2.position(buffer2.position() + 1).compact();

        var reader = BasePacketReader.accPacketReader();

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
    public void fragmentedAccPacket() {
        var packet = new AccPacket(1, 0, 42);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1);

        var oldLimit = buffer.limit();
        buffer.limit(buffer.position() + 2);
        var buffer2 = ByteBuffer.allocate(2).put(buffer);
        buffer.limit(oldLimit).compact();

        var reader = BasePacketReader.accPacketReader();

        assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer2));
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void errorNegReqPacker() {
        long job_id = -1;
        long range_start = 0;
        long range_end = 5;

        var buffer = ByteBuffer.allocate(Byte.BYTES + Long.BYTES*3);
        buffer.putLong(job_id).putLong(range_start).putLong(range_end);

        var reader = BasePacketReader.accPacketReader();
        // JOB_ID INVALID
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));

        long job_id_correct = 2;;
        buffer.clear();
        buffer.putLong(job_id_correct).putLong(range_end).putLong(range_start);
        reader.reset();

        // RANGE INVALID
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    public void errorGetAccPacket() {
        var reader = BasePacketReader.accPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }
}
