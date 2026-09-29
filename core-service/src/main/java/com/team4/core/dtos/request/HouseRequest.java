package com.team4.core.dtos.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;

/** Tracks field presence so PATCH distinguishes omitted fields from explicit null. */
@Getter
public class HouseRequest {
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private final Set<String> suppliedFields = new HashSet<>();

    @NotBlank @Size(max = 150)
    private String name;

    @NotBlank @Size(max = 255)
    private String addressStreet;

    @NotBlank @Size(max = 100)
    private String ward;

    @Size(max = 100)
    private String district;

    @NotBlank @Size(max = 100)
    private String city;

    @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 8)
    private BigDecimal latitude;

    @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 8)
    private BigDecimal longitude;

    @Size(max = 5000)
    private String description;

    public void setName(String value) {
        suppliedFields.add("name");
        this.name = value == null ? null : value.strip();
    }

    public void setAddressStreet(String value) {
        suppliedFields.add("addressStreet");
        this.addressStreet = value == null ? null : value.strip();
    }

    public void setWard(String value) {
        suppliedFields.add("ward");
        this.ward = value == null ? null : value.strip();
    }

    public void setDistrict(String value) {
        suppliedFields.add("district");
        this.district = value == null ? null : value.strip();
    }

    public void setCity(String value) {
        suppliedFields.add("city");
        this.city = value == null ? null : value.strip();
    }

    public void setLatitude(BigDecimal value) {
        suppliedFields.add("latitude");
        this.latitude = value;
    }

    public void setLongitude(BigDecimal value) {
        suppliedFields.add("longitude");
        this.longitude = value;
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

    @AssertTrue(message = "Vĩ độ và kinh độ phải cùng có giá trị hoặc cùng null")
    @JsonIgnore
    public boolean isCoordinatePairValid() {
        return (latitude == null) == (longitude == null);
    }

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
    }
}
