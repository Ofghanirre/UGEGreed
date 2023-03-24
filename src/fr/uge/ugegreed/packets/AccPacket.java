package fr.uge.ugegreed.packets;

import java.nio.ByteBuffer;

/**
 * Represents an ACC packet
 * @param job_id the job id to operate
 * @param range_start start range of number operated in (inclusive)
 * @param range_end end range of number operated in (exclusive)
 */
public record AccPacket(long job_id, long range_start, long range_end) implements Packet  {
    private static final byte CODE = PacketCode.UPDT.getCode();

    public AccPacket {
        if (job_id < 1) {
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
        return "ACC packet(job_id: " + job_id + ", range_start: " + range_start + ", range_end: " + range_end + ")";
    }
}
