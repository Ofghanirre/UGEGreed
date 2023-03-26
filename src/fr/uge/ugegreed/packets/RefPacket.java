package fr.uge.ugegreed.packets;

import java.nio.ByteBuffer;

/**
 * Represents an REF packet
 * @param job_id the job id operated
 * @param range_start start range of number operated in (inclusive)
 * @param range_end end range of number operated in (exclusive)
 */
public record RefPacket(long job_id, long range_start, long range_end) implements Packet  {
    private static final byte CODE = PacketCode.REF.getCode();

    public RefPacket {
        if (job_id < 0) {
            throw new IllegalArgumentException("job_id must be positive");
        }
        if (range_start > range_end) {
            throw new IllegalArgumentException("invalid range, range_start must be lower or equal to range_end");
        }
    }

    @Override
    public ByteBuffer toBuffer() {
        return ByteBuffer.allocate(Byte.BYTES + Long.BYTES*3)
                .put(CODE).putLong(job_id).putLong(range_start)
                .putLong(range_end).flip();
    }

    @Override
    public String toString() {
        return "REF packet(job_id: " + job_id + ", range_start: " + range_start + ", range_end: " + range_end + ")";
    }
}
