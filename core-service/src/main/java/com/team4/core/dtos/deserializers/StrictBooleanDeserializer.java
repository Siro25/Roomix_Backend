package com.team4.core.dtos.deserializers;

import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class StrictBooleanDeserializer extends ValueDeserializer<Boolean> {
    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() != JsonToken.VALUE_TRUE && parser.currentToken() != JsonToken.VALUE_FALSE) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return parser.currentToken() == JsonToken.VALUE_TRUE;
    }
}
