package com.test1.test1.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContactUploadErrorDto {
    private String phoneNumber;
    private String reason;
}