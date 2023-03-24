package fr.uge.ugegreed.packets;

import java.nio.ByteBuffer;

/**
 * Represents a TCP packet
 */
public sealed interface Packet permits AccPacket, InitPacket, ReqPacket, UpdtPacket {
  /**
   * Defines the packet types and associated code
   */
  enum PacketCode {
    INIT(1), UPDT(2), REQ(3), ACC(4), REF(5), ANS(6), REDI(7), DISC(8), OK_DISC(9);

    private final int code;
    PacketCode(int code) {
      this.code = code;
    }

    byte getCode() {
      return (byte) code;
    }
  }

  /**
   * Transforms the packet into ByteBuffer form, in a read state.
   * @return ByteBuffer containing the packet
   */
  ByteBuffer toBuffer();
}
