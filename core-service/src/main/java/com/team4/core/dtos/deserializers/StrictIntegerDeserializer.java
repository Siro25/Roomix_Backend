package com.team4.core.dtos.deserializers;

import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Prevents JSON decimals or strings from being silently coerced into an integer. */
public class StrictIntegerDeserializer extends ValueDeserializer<Integer> {
    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return parser.getIntValue();
    }
}
