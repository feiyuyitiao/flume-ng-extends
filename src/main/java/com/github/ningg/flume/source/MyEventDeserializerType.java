package com.github.ningg.flume.source;

import org.apache.flume.annotations.InterfaceAudience;
import org.apache.flume.annotations.InterfaceStability;
import org.apache.flume.serialization.AvroEventDeserializer;
import org.apache.flume.serialization.EventDeserializer;
import org.apache.flume.serialization.LineDeserializer;

@InterfaceAudience.Private
@InterfaceStability.Unstable
public enum MyEventDeserializerType {
  LINE(LineDeserializer.Builder.class),
  WRAPLINE(WrapLineDeserializer.Builder.class),
  AVRO(AvroEventDeserializer.Builder.class),
  OTHER(null);

  private final Class<? extends EventDeserializer.Builder> builderClass;

  MyEventDeserializerType(Class<? extends EventDeserializer.Builder> builderClass) {
    this.builderClass = builderClass;
  }

  public Class<? extends EventDeserializer.Builder> getBuilderClass() {
    return builderClass;
  }

}
