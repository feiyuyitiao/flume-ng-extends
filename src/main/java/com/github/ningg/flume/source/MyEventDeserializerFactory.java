package com.github.ningg.flume.source;

import org.apache.flume.Context;
import org.apache.flume.FlumeException;
import org.apache.flume.annotations.InterfaceAudience;
import org.apache.flume.annotations.InterfaceStability;
import org.apache.flume.serialization.EventDeserializer;
import org.apache.flume.serialization.EventDeserializerType;
import org.apache.flume.serialization.ResettableInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

@InterfaceAudience.Private
@InterfaceStability.Stable
public class MyEventDeserializerFactory {

	private static final Logger logger = LoggerFactory.getLogger(MyEventDeserializerFactory.class);

	public static EventDeserializer getInstance(String deserializerType, Context context, ResettableInputStream in) {

		Preconditions.checkNotNull(deserializerType, "serializer type must not be null");

		// try to find builder class in enum of known output serializers
		MyEventDeserializerType type;
		try {
			type = MyEventDeserializerType.valueOf(deserializerType.toUpperCase());
		} catch (IllegalArgumentException e) {
			logger.debug("Not in enum, loading builder class: {}", deserializerType);
			type = MyEventDeserializerType.OTHER;
		}
		Class<? extends EventDeserializer.Builder> builderClass = type.getBuilderClass();

		// handle the case where they have specified their own builder in the
		// config
		if (builderClass == null) {
			try {
				Class c = Class.forName(deserializerType);
				if (c != null && EventDeserializer.Builder.class.isAssignableFrom(c)) {
					builderClass = (Class<? extends EventDeserializer.Builder>) c;
				} else {
					String errMessage = "Unable to instantiate Builder from " + deserializerType + ": does not appear to implement "
							+ EventDeserializer.Builder.class.getName();
					throw new FlumeException(errMessage);
				}
			} catch (ClassNotFoundException ex) {
				logger.error("Class not found: " + deserializerType, ex);
				throw new FlumeException(ex);
			}
		}

		// build the builder
		EventDeserializer.Builder builder;
		try {
			builder = builderClass.newInstance();
		} catch (InstantiationException ex) {
			String errMessage = "Cannot instantiate builder: " + deserializerType;
			logger.error(errMessage, ex);
			throw new FlumeException(errMessage, ex);
		} catch (IllegalAccessException ex) {
			String errMessage = "Cannot instantiate builder: " + deserializerType;
			logger.error(errMessage, ex);
			throw new FlumeException(errMessage, ex);
		}

		return builder.build(context, in);
	}

}
