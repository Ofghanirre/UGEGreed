package fr.uge.ugegreed.tests.basePacket;

import fr.uge.ugegreed.packets.DiscPacket;
import fr.uge.ugegreed.readers.DiscPacketReader;
import fr.uge.ugegreed.readers.Reader;
import fr.uge.ugegreed.utils.TypeToByteWriter;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DiscPacketTest {
    @Test
    public void simpleDiscPacket() {
        var packet = new DiscPacket(
                1, 1, new DiscPacket.InnerDiscPacket[]{
                        new DiscPacket.InnerDiscPacket(1, 7777)
        });
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var reader = new DiscPacketReader();

        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void resetDiscPacket() {
        var packet = new DiscPacket(
                1, 1, new DiscPacket.InnerDiscPacket[]{
                new DiscPacket.InnerDiscPacket(1, 7777)
        });
        var packet2 = new DiscPacket(
                1, 1, new DiscPacket.InnerDiscPacket[]{
                new DiscPacket.InnerDiscPacket(1, 6666)
        });
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var buffer2 = packet2.toBuffer();
        buffer2.position(buffer2.position() + 1).compact();

        var reader = new DiscPacketReader();

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
    public void fragmentedDiscPacket() {
        var packet = new DiscPacket(
                1, 1, new DiscPacket.InnerDiscPacket[]{
                new DiscPacket.InnerDiscPacket(1,7777)
        });
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1);

        var oldLimit = buffer.limit();
        buffer.limit(buffer.position() + 2);
        var buffer2 = ByteBuffer.allocate(2).put(buffer);
        buffer.limit(oldLimit).compact();

        var reader = new DiscPacketReader();

        assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer2));
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void errorNegDiscPacker() {
        int nb_reco = -1;
        int nb_jobs = 1;
        var job = TypeToByteWriter.getHost(new InetSocketAddress("42.69.00.30", 7777));

        var buffer = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES*2 + DiscPacket.InnerDiscPacket.getSize())
                .putInt(nb_reco).putInt(nb_jobs)
                .put(job);


        var reader = new DiscPacketReader();
        // nb_reco INVALID
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));

        reader.reset();
        buffer.clear();

        nb_reco = 1;
        nb_jobs = -1;
        buffer.putInt(nb_reco).putInt(nb_jobs).put(job);
        // nb_jobs INVALID
        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    public void errorGetInitPacket() {
        var reader = new DiscPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }
}
