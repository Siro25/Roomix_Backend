package com.team4.core.dtos.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team4.core.dtos.deserializers.StrictIntegerDeserializer;
import com.team4.core.dtos.deserializers.StrictStringDeserializer;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Preserves explicit null values separately from fields omitted in a PATCH. */
@Getter
public class FloorRequest {
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private final Set<String> suppliedFields = new HashSet<>();

    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    @NotNull
    @Min(0)
    private Integer floorNumber;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    @NotBlank
    @Size(max = 100)
    private String name;

    @JsonDeserialize(using = StrictStringDeserializer.class)
    @Size(max = 255)
    private String description;

    public void setFloorNumber(Integer value) {
        suppliedFields.add("floorNumber");
        this.floorNumber = value;
    }

    public void setName(String value) {
        suppliedFields.add("name");
        this.name = value == null ? null : value.strip();
    }

    public void setDescription(String value) {
        suppliedFields.add("description");
        this.description = value == null ? null : value.strip();
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
