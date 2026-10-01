package com.team4.core.dtos.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team4.core.dtos.deserializers.StrictBooleanDeserializer;
import com.team4.core.dtos.deserializers.StrictDecimalDeserializer;
import com.team4.core.dtos.deserializers.StrictIntegerDeserializer;
import com.team4.core.dtos.deserializers.StrictStringDeserializer;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Distinguishes omitted PATCH fields from explicit null; booleans default to false on creation. */
@Getter
public class RoomRequest {
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private final Set<String> suppliedFields = new HashSet<>();

    @JsonDeserialize(using = StrictStringDeserializer.class)
    @NotBlank
    @Size(max = 50)
    private String roomNumber;

    @JsonDeserialize(using = StrictDecimalDeserializer.class)
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal area;

    @JsonDeserialize(using = StrictDecimalDeserializer.class)
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 13, fraction = 2)
    private BigDecimal basePrice;

    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    @NotNull
    @Min(1)
    private Integer maxTenants;

    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    @NotNull
    private Boolean hasPrivateBathroom = false;

    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    @NotNull
    private Boolean hasAirConditioner = false;

    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    @NotNull
    private Boolean hasWaterHeater = false;

    @JsonDeserialize(using = StrictBooleanDeserializer.class)
    @NotNull
    private Boolean hasBalcony = false;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    @Size(max = 5000)
    private String amenitiesDescription;

    public void setRoomNumber(String value) {
        suppliedFields.add("roomNumber");
        this.roomNumber = value == null ? null : value.strip().toUpperCase(Locale.ROOT);
    }

    public void setArea(BigDecimal value) {
        suppliedFields.add("area");
        this.area = value;
    }

    public void setBasePrice(BigDecimal value) {
        suppliedFields.add("basePrice");
        this.basePrice = value;
    }

    public void setMaxTenants(Integer value) {
        suppliedFields.add("maxTenants");
        this.maxTenants = value;
    }

    public void setHasPrivateBathroom(Boolean value) {
        suppliedFields.add("hasPrivateBathroom");
        this.hasPrivateBathroom = value;
    }

    public void setHasAirConditioner(Boolean value) {
        suppliedFields.add("hasAirConditioner");
        this.hasAirConditioner = value;
    }

    public void setHasWaterHeater(Boolean value) {
        suppliedFields.add("hasWaterHeater");
        this.hasWaterHeater = value;
    }

    public void setHasBalcony(Boolean value) {
        suppliedFields.add("hasBalcony");
        this.hasBalcony = value;
    }

    public void setAmenitiesDescription(String value) {
        suppliedFields.add("amenitiesDescription");
        this.amenitiesDescription = value == null ? null : value.strip();
    }

    public boolean hasField(String field) {
        return suppliedFields.contains(field);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return suppliedFields.isEmpty();
    }

    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }
}
