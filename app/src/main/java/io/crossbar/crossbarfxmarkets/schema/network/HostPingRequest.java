// automatically generated by the FlatBuffers compiler, do not modify

package io.crossbar.crossbarfxmarkets.schema.network;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class HostPingRequest extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_1_12_0(); }
  public static HostPingRequest getRootAsHostPingRequest(ByteBuffer _bb) { return getRootAsHostPingRequest(_bb, new HostPingRequest()); }
  public static HostPingRequest getRootAsHostPingRequest(ByteBuffer _bb, HostPingRequest obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public HostPingRequest __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  /**
   * DNS hostname or IPv4/v6 host address of the host to be probed with ICMP pings.
   */
  public String host() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer hostAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer hostInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }
  /**
   * Total number of ping requests sent.
   */
  public int total() { int o = __offset(6); return o != 0 ? bb.getShort(o + bb_pos) & 0xFFFF : 0; }
  /**
   * Parallel ping requests being sent (while the probing is running).
   */
  public int parallel() { int o = __offset(8); return o != 0 ? bb.getShort(o + bb_pos) & 0xFFFF : 0; }
  /**
   * Delay between rounds of pings being sent in ms.
   */
  public int delay() { int o = __offset(10); return o != 0 ? bb.getShort(o + bb_pos) & 0xFFFF : 0; }
  /**
   * Random jitter of delay between rounds of pings.
   */
  public int delayJitter() { int o = __offset(12); return o != 0 ? bb.getShort(o + bb_pos) & 0xFFFF : 0; }

  public static int createHostPingRequest(FlatBufferBuilder builder,
      int hostOffset,
      int total,
      int parallel,
      int delay,
      int delay_jitter) {
    builder.startTable(5);
    HostPingRequest.addHost(builder, hostOffset);
    HostPingRequest.addDelayJitter(builder, delay_jitter);
    HostPingRequest.addDelay(builder, delay);
    HostPingRequest.addParallel(builder, parallel);
    HostPingRequest.addTotal(builder, total);
    return HostPingRequest.endHostPingRequest(builder);
  }

  public static void startHostPingRequest(FlatBufferBuilder builder) { builder.startTable(5); }
  public static void addHost(FlatBufferBuilder builder, int hostOffset) { builder.addOffset(0, hostOffset, 0); }
  public static void addTotal(FlatBufferBuilder builder, int total) { builder.addShort(1, (short)total, (short)0); }
  public static void addParallel(FlatBufferBuilder builder, int parallel) { builder.addShort(2, (short)parallel, (short)0); }
  public static void addDelay(FlatBufferBuilder builder, int delay) { builder.addShort(3, (short)delay, (short)0); }
  public static void addDelayJitter(FlatBufferBuilder builder, int delayJitter) { builder.addShort(4, (short)delayJitter, (short)0); }
  public static int endHostPingRequest(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public HostPingRequest get(int j) { return get(new HostPingRequest(), j); }
    public HostPingRequest get(HostPingRequest obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}
