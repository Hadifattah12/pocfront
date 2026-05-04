package com.test1.test1.dtos;

import com.test1.test1.enums.ContactOperation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class ContactChangeDto {

    @NotBlank
    private String phoneNumber;

    private String contactName;

//    @NotNull
//    private ContactOperation operation;

//    @NotNull
//    private Instant changedAt;
}