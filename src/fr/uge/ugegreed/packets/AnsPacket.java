package fr.uge.ugegreed.packets;

import fr.uge.ugegreed.utils.TypeToByteWriter;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Represents an ANS packet
 * @param job_id the job id operated
 * @param number the number computed
 * @param result the resulting string, encoded in UTF-8
 */
public record AnsPacket(long job_id, long number, String result) implements Packet  {
    private static final byte CODE = PacketCode.ANS.getCode();

    public AnsPacket {
        if (job_id < 0) {
            throw new IllegalArgumentException("job_id must be positive");
        }
        Objects.requireNonNull(result);
    }

    @Override
    public ByteBuffer toBuffer() {
        var result_bb = TypeToByteWriter.getString(result);
        return ByteBuffer.allocate(Byte.BYTES + Long.BYTES*2 + result_bb.remaining())
                .put(CODE).putLong(job_id).put(result_bb);
    }

    @Override
    public String toString() {
        return "ANS packet(job_id: " + job_id + ", number: " + number + ", result: " + result + ")";
    }
}
