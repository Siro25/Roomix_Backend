package com.team4.core.dtos.deserializers;

import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Accepts JSON strings without coercing numbers or booleans into text. */
public class StrictStringDeserializer extends ValueDeserializer<String> {
    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() != JsonToken.VALUE_STRING) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return parser.getString();
    }
}
