package fr.uge.ugegreed.tests;

import fr.uge.ugegreed.packets.*;
import fr.uge.ugegreed.readers.BasePacketReader;
import fr.uge.ugegreed.readers.Reader;
import fr.uge.ugegreed.utils.TypeToByteWriter;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BasePacketReaderTest {
    // ===============================================================
    // INIT PACKET
    // ===============================================================
    @Test
    public void simpleInitPacket() {
        var packet = new InitPacket(3);
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
        var packet = new InitPacket(42);
        var packet2 = new InitPacket(3333);
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
        var packet = new InitPacket(3);
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
        var buffer = ByteBuffer.allocate(4).putInt(-2);
        var reader = BasePacketReader.initPacketReader();

        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    public void errorGetInitPacket() {
        var reader = BasePacketReader.initPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }


    // ===============================================================
    // UPDT PACKET
    // ===============================================================
    @Test
    public void simpleUpdtPacket() {
        var packet = new UpdtPacket(1);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var reader = BasePacketReader.updtPacketReader();

        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void resetUpdtPacket() {
        var packet = new UpdtPacket(42);
        var packet2 = new UpdtPacket(3333);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var buffer2 = packet2.toBuffer();
        buffer2.position(buffer2.position() + 1).compact();

        var reader = BasePacketReader.updtPacketReader();

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
    public void fragmentedUpdtPacket() {
        var packet = new UpdtPacket(3);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1);

        var oldLimit = buffer.limit();
        buffer.limit(buffer.position() + 2);
        var buffer2 = ByteBuffer.allocate(2).put(buffer);
        buffer.limit(oldLimit).compact();

        var reader = BasePacketReader.updtPacketReader();

        assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer2));
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void errorNegUpdtPacker() {
        var buffer = ByteBuffer.allocate(4).putInt(-2);
        var reader = BasePacketReader.updtPacketReader();

        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    public void errorGetUpdtPacket() {
        var reader = BasePacketReader.updtPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }

    // ===============================================================
    // REQ PACKET
    // ===============================================================
    @Test
    public void simpleReqPacket() {
        var packet = new ReqPacket(1, "testURL", "testMain", 0, 42);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var reader = BasePacketReader.reqPacketReader();

        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void resetReqPacket() {
        var packet = new ReqPacket(1, "testURL", "testMain", 0, 42);
        var packet2 = new ReqPacket(2, "testURL2", "testMain2", 200, 242);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1).compact();
        var buffer2 = packet2.toBuffer();
        buffer2.position(buffer2.position() + 1).compact();

        var reader = BasePacketReader.reqPacketReader();

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
    public void fragmentedReqPacket() {
        var packet = new ReqPacket(1, "testURL", "testMain", 0, 42);
        var buffer = packet.toBuffer();
        buffer.position(buffer.position() + 1);

        var oldLimit = buffer.limit();
        buffer.limit(buffer.position() + 2);
        var buffer2 = ByteBuffer.allocate(2).put(buffer);
        buffer.limit(oldLimit).compact();

        var reader = BasePacketReader.reqPacketReader();

        assertEquals(Reader.ProcessStatus.REFILL, reader.process(buffer2));
        assertEquals(Reader.ProcessStatus.DONE, reader.process(buffer));
        assertEquals(packet, reader.get());
        assertEquals(0, buffer.position());
        assertEquals(buffer.capacity(), buffer.limit());
    }

    @Test
    public void errorNegReqPacker() {
    long job_id = -1;
    String jar_Url = "test";
    String main = "test";
    long range_start = 4;
    long range_end = 5;
    var jar_url_bb = TypeToByteWriter.getString(jar_Url);
    var main_bb = TypeToByteWriter.getString(main);

    var buffer = ByteBuffer.allocate(Byte.BYTES + Long.BYTES*3 + jar_url_bb.remaining() + main_bb.remaining());
    buffer.put(Packet.PacketCode.REQ.getCode()).putLong(job_id).put(jar_url_bb).put(main_bb).putLong(range_start).putLong(range_end);

    var reader = BasePacketReader.reqPacketReader();

    assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }

    @Test
    public void errorGetReqPacket() {
        var reader = BasePacketReader.reqPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }

    // ===============================================================
    // ACC PACKET
    // ===============================================================
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

/*    @Test
    public void errorNegAccPacker() {
        long job_id = -5;
        long range_start = 7;
        long range_end = 6;

        var buffer = ByteBuffer.allocate(Byte.BYTES + Long.BYTES*3);
        buffer.put(Packet.PacketCode.REQ.getCode()).putLong(job_id).putLong(range_start).putLong(range_end);

        var reader = BasePacketReader.accPacketReader();

        assertEquals(Reader.ProcessStatus.ERROR, reader.process(buffer));
    }*/

    @Test
    public void errorGetAccPacket() {
        var reader = BasePacketReader.accPacketReader();
        assertThrows(IllegalStateException.class, reader::get);
    }
}