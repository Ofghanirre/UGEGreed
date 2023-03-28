package fr.uge.ugegreed.packets;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Represents a TCP packet
 */
public sealed interface Packet permits AccPacket, AnsPacket, DiscPacket, InitPacket, OkDiscPacket, RediPacket, RefPacket, ReqPacket, UpdtPacket {
  /**
   * Defines the packet types and associated code
   */
  enum PacketCode {
    INIT(1), UPDT(2), REQ(3), ACC(4), REF(5), ANS(6), REDI(7), DISC(8), OK_DISC(9);

    private final int code;
    PacketCode(int code) {
      this.code = code;
    }

    public byte getCode() {
      return (byte) code;
    }

    /**
     * Returns the packet type associated to given byte value
     * @param b byte value
     * @return packet type associated to given byte value
     */
    static public Optional<PacketCode> getPacketCodeFromByte(Byte b) {
      return switch (b) {
        case 1 -> Optional.of(INIT);
        case 2 -> Optional.of(UPDT);
        case 3 -> Optional.of(REQ);
        case 4 -> Optional.of(ACC);
        case 5 -> Optional.of(REF);
        case 6 -> Optional.of(ANS);
        case 7 -> Optional.of(REDI);
        case 8 -> Optional.of(DISC);
        case 9 -> Optional.of(OK_DISC);
        default -> Optional.empty();
      };
    }
  }

  /**
   * Transforms the packet into ByteBuffer form, in a read state.
   * @return ByteBuffer containing the packet
   */
  ByteBuffer toBuffer();
}
