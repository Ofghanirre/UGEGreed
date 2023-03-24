package fr.uge.ugegreed.packets;

import fr.uge.ugegreed.utils.TypeToByteWriter;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a DISC packet
 *
 * @param nb_reco Number of connection to expect after a deconnection (essentially 0 for children, n for parent)
 * @param nb_jobs Number of jobs to read
 * @param jobs Jobs to read, consist of an Long and an Host (InetSocketAddress) (6 bytes on ByteBuffer)
 */
public record DiscPacket(int nb_reco, int nb_jobs, InnerDiscPacket[] jobs) implements Packet {

  public record InnerDiscPacket(long job_id, InetSocketAddress new_upstream) {
    public InnerDiscPacket {
      if (job_id < 1) {
        throw new IllegalArgumentException("job_id must be positive");
      }
      Objects.requireNonNull(new_upstream);
    }

    /**
     * Return a ByteBuffer representing the Object
     * @return ByteBuffer in READ mode
     */
    public ByteBuffer toBuffer() {
      return ByteBuffer.allocate(getSize()).putLong(job_id).put(TypeToByteWriter.getHost(new_upstream)).flip();
    }

    public static int getSize() {
      return Long.BYTES + 6;
    }
  }

  private static final byte CODE = PacketCode.DISC.getCode();

  public DiscPacket {
    if (nb_reco < 0) {
      throw new IllegalArgumentException("nb_reco must be positive");
    }
    if (nb_jobs < 0) {
      throw new IllegalArgumentException("nb_jobs must be positive");
    }
    if (jobs.length != nb_jobs) {
      throw new IllegalArgumentException("the given jobs do not respects the nb_jobs information");
    }
  }

  @Override
  public ByteBuffer toBuffer() {
    ByteBuffer result = ByteBuffer.allocate(Byte.BYTES + Integer.BYTES*2 + InnerDiscPacket.getSize() * nb_jobs)
            .put(CODE).putInt(nb_reco).putInt(nb_jobs);
    for (InnerDiscPacket job : jobs) {
      result.put(job.toBuffer());
    }
    return result;
  }

  @Override
  public String toString() {
    return "DISC packet(nb_reco: " + nb_reco + ", nb_jobs: " + nb_jobs + ", jobs:" + Arrays.toString(jobs) + ")";
  }
}
