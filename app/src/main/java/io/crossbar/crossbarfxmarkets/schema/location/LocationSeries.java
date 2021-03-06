// automatically generated by the FlatBuffers compiler, do not modify

package io.crossbar.crossbarfxmarkets.schema.location;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
/**
 * A series of location samples.
 */
public final class LocationSeries extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_1_12_0(); }
  public static LocationSeries getRootAsLocationSeries(ByteBuffer _bb) { return getRootAsLocationSeries(_bb, new LocationSeries()); }
  public static LocationSeries getRootAsLocationSeries(ByteBuffer _bb, LocationSeries obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public LocationSeries __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  /**
   * Persistent (static) ID of the entity tracking its location.
   */
  public int entity(int j) { int o = __offset(4); return o != 0 ? bb.get(__vector(o) + j * 1) & 0xFF : 0; }
  public int entityLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }
  public ByteVector entityVector() { return entityVector(new ByteVector()); }
  public ByteVector entityVector(ByteVector obj) { int o = __offset(4); return o != 0 ? obj.__assign(__vector(o), bb) : null; }
  public ByteBuffer entityAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer entityInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }
  /**
   * Samples: point in time (UTC) when sample was measured, in Unix time (ns precision).
   */
  public long timestamp(int j) { int o = __offset(6); return o != 0 ? bb.getLong(__vector(o) + j * 8) : 0; }
  public int timestampLength() { int o = __offset(6); return o != 0 ? __vector_len(o) : 0; }
  public LongVector timestampVector() { return timestampVector(new LongVector()); }
  public LongVector timestampVector(LongVector obj) { int o = __offset(6); return o != 0 ? obj.__assign(__vector(o), bb) : null; }
  public ByteBuffer timestampAsByteBuffer() { return __vector_as_bytebuffer(6, 8); }
  public ByteBuffer timestampInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 6, 8); }
  /**
   * Samples: WGS84 longitude, rounded to 7 decimals.
   */
  public double lon(int j) { int o = __offset(8); return o != 0 ? bb.getDouble(__vector(o) + j * 8) : 0; }
  public int lonLength() { int o = __offset(8); return o != 0 ? __vector_len(o) : 0; }
  public DoubleVector lonVector() { return lonVector(new DoubleVector()); }
  public DoubleVector lonVector(DoubleVector obj) { int o = __offset(8); return o != 0 ? obj.__assign(__vector(o), bb) : null; }
  public ByteBuffer lonAsByteBuffer() { return __vector_as_bytebuffer(8, 8); }
  public ByteBuffer lonInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 8, 8); }
  /**
   * Samples: WGS84 latitude, rounded to 7 decimals.
   */
  public double lat(int j) { int o = __offset(10); return o != 0 ? bb.getDouble(__vector(o) + j * 8) : 0; }
  public int latLength() { int o = __offset(10); return o != 0 ? __vector_len(o) : 0; }
  public DoubleVector latVector() { return latVector(new DoubleVector()); }
  public DoubleVector latVector(DoubleVector obj) { int o = __offset(10); return o != 0 ? obj.__assign(__vector(o), bb) : null; }
  public ByteBuffer latAsByteBuffer() { return __vector_as_bytebuffer(10, 8); }
  public ByteBuffer latInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 10, 8); }

  public static int createLocationSeries(FlatBufferBuilder builder,
      int entityOffset,
      int timestampOffset,
      int lonOffset,
      int latOffset) {
    builder.startTable(4);
    LocationSeries.addLat(builder, latOffset);
    LocationSeries.addLon(builder, lonOffset);
    LocationSeries.addTimestamp(builder, timestampOffset);
    LocationSeries.addEntity(builder, entityOffset);
    return LocationSeries.endLocationSeries(builder);
  }

  public static void startLocationSeries(FlatBufferBuilder builder) { builder.startTable(4); }
  public static void addEntity(FlatBufferBuilder builder, int entityOffset) { builder.addOffset(0, entityOffset, 0); }
  public static int createEntityVector(FlatBufferBuilder builder, byte[] data) { return builder.createByteVector(data); }
  public static int createEntityVector(FlatBufferBuilder builder, ByteBuffer data) { return builder.createByteVector(data); }
  public static void startEntityVector(FlatBufferBuilder builder, int numElems) { builder.startVector(1, numElems, 1); }
  public static void addTimestamp(FlatBufferBuilder builder, int timestampOffset) { builder.addOffset(1, timestampOffset, 0); }
  public static int createTimestampVector(FlatBufferBuilder builder, long[] data) { builder.startVector(8, data.length, 8); for (int i = data.length - 1; i >= 0; i--) builder.addLong(data[i]); return builder.endVector(); }
  public static void startTimestampVector(FlatBufferBuilder builder, int numElems) { builder.startVector(8, numElems, 8); }
  public static void addLon(FlatBufferBuilder builder, int lonOffset) { builder.addOffset(2, lonOffset, 0); }
  public static int createLonVector(FlatBufferBuilder builder, double[] data) { builder.startVector(8, data.length, 8); for (int i = data.length - 1; i >= 0; i--) builder.addDouble(data[i]); return builder.endVector(); }
  public static void startLonVector(FlatBufferBuilder builder, int numElems) { builder.startVector(8, numElems, 8); }
  public static void addLat(FlatBufferBuilder builder, int latOffset) { builder.addOffset(3, latOffset, 0); }
  public static int createLatVector(FlatBufferBuilder builder, double[] data) { builder.startVector(8, data.length, 8); for (int i = data.length - 1; i >= 0; i--) builder.addDouble(data[i]); return builder.endVector(); }
  public static void startLatVector(FlatBufferBuilder builder, int numElems) { builder.startVector(8, numElems, 8); }
  public static int endLocationSeries(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public LocationSeries get(int j) { return get(new LocationSeries(), j); }
    public LocationSeries get(LocationSeries obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

