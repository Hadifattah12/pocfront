package com.test1.test1.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UploadContactsBatchRequest {

    @Valid

    private List<ContactChangeDto> added = new ArrayList<>();

    @Valid

    private List<ContactChangeDto> updated = new ArrayList<>();

    @Valid

    private List<ContactChangeDto> deleted = new ArrayList<>();

}
