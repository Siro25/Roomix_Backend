package com.team4.core.dtos.deserializers;

import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class StrictDecimalDeserializer extends ValueDeserializer<java.math.BigDecimal> {
    @Override
    public java.math.BigDecimal deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT && parser.currentToken() != JsonToken.VALUE_NUMBER_FLOAT) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        return parser.getDecimalValue();
    }
}
