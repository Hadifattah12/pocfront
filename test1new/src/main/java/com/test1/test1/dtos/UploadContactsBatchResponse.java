package com.test1.test1.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UploadContactsBatchResponse {

    private boolean success = true;

    private int received;
//    private int inserted;
//    private int updated;
//    private int deleted;
//    private int ignored;
//    private int failed;

    private List<ContactUploadErrorDto> errors = new ArrayList<>();
}