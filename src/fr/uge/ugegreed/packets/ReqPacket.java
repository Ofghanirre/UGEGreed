package fr.uge.ugegreed.packets;

import fr.uge.ugegreed.utils.TypeToByteWriter;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Represents a REQ packet
 * @param job_id the job id to operate
 * @param jar_URL path to the jar to execute
 * @param class_name name of the main class to execute
 * @param range_start start range of number to operate on (inclusive)
 * @param range_end end range of number to operate on (exclusive)
 */
public record ReqPacket(long job_id, String jar_URL, String class_name, long range_start, long range_end) implements Packet  {
    private static final byte CODE = PacketCode.UPDT.getCode();

    public ReqPacket {
        if (job_id < 1) {
            throw new IllegalArgumentException("job_id must be positive");
        }
        Objects.requireNonNull(jar_URL);
        Objects.requireNonNull(class_name);
        if (range_start > range_end) {
            throw new IllegalArgumentException("invalid range, range_start must be lower or equal to range_end");
        }
    }

    @Override
    public ByteBuffer toBuffer() {
        var jar_url_bb = TypeToByteWriter.getString(jar_URL);
        var class_name_bb = TypeToByteWriter.getString(jar_URL);
        return ByteBuffer.allocate(Byte.BYTES + Long.BYTES*3 + jar_url_bb.capacity() + class_name_bb.capacity())
                .put(CODE).putLong(job_id).put(jar_url_bb.flip()).put(class_name_bb.flip()).putLong(range_start)
                .putLong(range_end).flip();
    }

    @Override
    public String toString() {
        return "REQ packet(job_id: " + job_id + ", jar_URL: " + jar_URL + ", class_name: " + class_name + ", range_start: " + range_start + ", range_end: " + range_end + ")";
    }
}
