package fr.uge.ugegreed.tests.basePacket;

import fr.uge.ugegreed.packets.AnsPacket;
import fr.uge.ugegreed.readers.BasePacketReader;
import fr.uge.ugegreed.readers.Reader;
import fr.uge.ugegreed.utils.TypeToByteWriter;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AnsPacketTest {
    @Test
    public void simpleAnsPacket() {
        var packet = new AnsPacket(1,  42, "42");
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var reader = BasePacketReader.ansPacketReader();

        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void resetAnsPacket() {
        var packet = new AnsPacket(1,  0, "42");
        var packet2 = new AnsPacket(2,  200, "242");
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var buffer2 = packet2.toBuffer();
        buffer2.position(buffer2.position() + 1).compact();

        var reader = BasePacketReader.ansPacketReader();

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
    public void fragmentedAnsPacket() {
        var packet = new AnsPacket(1, 0, "42");
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1);

        var oldLimit = buffer.limit();
        buffer.limit(buffer.position() + 2);
        var buffer2 = ByteBuffer.allocate(2).put(buffer);
        buffer.limit(oldLimit).compact();

        var reader = BasePacketReader.ansPacketReader();

        assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer2));
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void errorNegAnsPacker() {
        long job_id = -1;
        long number = 0;
        String result = "42";
        var result_bb = TypeToByteWriter.getString(result);


        var buffer = ByteBuffer.allocate(Byte.BYTES + Long.BYTES*2 + result_bb.remaining());
        buffer.putLong(job_id).putLong(number).put(result_bb);

        var reader = BasePacketReader.ansPacketReader();
        // JOB_ID INVALID
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    public void errorGetAnsPacket() {
        var reader = BasePacketReader.ansPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }
}
