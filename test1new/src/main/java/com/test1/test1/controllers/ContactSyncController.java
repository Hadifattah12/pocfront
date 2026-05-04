package com.test1.test1.controllers;

import com.test1.test1.Services.ContactSyncService;
import com.test1.test1.dtos.UploadContactsBatchRequest;
import com.test1.test1.dtos.UploadContactsBatchResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
public class ContactSyncController {

    private final ContactSyncService contactSyncService;

    public ContactSyncController(ContactSyncService contactSyncService) {
        this.contactSyncService = contactSyncService;
    }

    @PostMapping("/batch")
    public UploadContactsBatchResponse uploadBatch(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid UploadContactsBatchRequest request
    ) {
        return contactSyncService.uploadBatch(userId, request);
    }
}