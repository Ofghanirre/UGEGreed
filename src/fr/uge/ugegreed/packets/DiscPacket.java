package fr.uge.ugegreed.packets;

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

  public record InnerDiscPacket(long job_id, int new_upstream) {
    public InnerDiscPacket {
      if (job_id < 0) {
        throw new IllegalArgumentException("job_id must be positive");
      }
    }

    /**
     * Return a ByteBuffer representing the Object
     * @return ByteBuffer in READ mode
     */
    public ByteBuffer toBuffer() {
      return ByteBuffer.allocate(getSize()).putLong(job_id).putInt(new_upstream).flip();
    }

    public static int getSize() {
      return Long.BYTES + Integer.BYTES;
    }

    @Override
    public String toString() {
      return "job_id: " + job_id + ", new_upstream: " + new_upstream;
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
    return result.flip();
  }

  @Override
  public String toString() {
    return "DISC packet(nb_reco: " + nb_reco + ", nb_jobs: " + nb_jobs + ", jobs: " + Arrays.toString(jobs) + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DiscPacket that = (DiscPacket) o;
    return nb_reco == that.nb_reco && nb_jobs == that.nb_jobs && Arrays.equals(jobs, that.jobs);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(nb_reco, nb_jobs);
    result = 31 * result + Arrays.hashCode(jobs);
    return result;
  }
}
