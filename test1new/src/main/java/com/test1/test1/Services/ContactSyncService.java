package com.test1.test1.Services;

import com.test1.test1.dtos.*;
import com.test1.test1.enums.ContactStatus;
import com.test1.test1.models.UserContact;
import com.test1.test1.repositories.UserContactRepository;
import com.test1.test1.utils.PhoneNumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ContactSyncService {

    private final UserContactRepository userContactRepository;
    private final ContactHashService contactHashService;

    public ContactSyncService(
            UserContactRepository userContactRepository,
            ContactHashService contactHashService
    ) {
        this.userContactRepository = userContactRepository;
        this.contactHashService = contactHashService;
    }

    @Transactional
    public UploadContactsBatchResponse uploadBatch(
            Long userId,
            UploadContactsBatchRequest request
    ) {
        UploadContactsBatchResponse response = new UploadContactsBatchResponse();

        response.setReceived(
                safeSize(request.getAdded()) +
                        safeSize(request.getUpdated()) +
                        safeSize(request.getDeleted())
        );

        processUpserts(userId, request.getAdded(), response);
        processUpserts(userId, request.getUpdated(), response);
        processDeleted(userId, request.getDeleted(), response);

        return response;
    }

    private void processUpserts(
            Long userId,
            List<ContactChangeDto> contacts,
            UploadContactsBatchResponse response
    ) {
        if (contacts == null) return;

        for (ContactChangeDto dto : contacts) {
            try {
                upsertActiveContact(userId, dto);
            } catch (Exception e) {
                markFailed(dto, response);
            }
        }
    }

    private void processDeleted(
            Long userId,
            List<ContactChangeDto> contacts,
            UploadContactsBatchResponse response
    ) {
        if (contacts == null) return;

        for (ContactChangeDto dto : contacts) {
            try {
                softDeleteContact(userId, dto);
            } catch (Exception e) {
                markFailed(dto, response);
            }
        }
    }

    private void upsertActiveContact(
            Long userId,
            ContactChangeDto dto
    ) {
        byte[] phoneHash = normalizeAndHash(dto.getPhoneNumber(), null);

        UserContact contact = userContactRepository
                .findByOwnerUserIdAndContactPhoneHash(userId, phoneHash)
                .orElseGet(() -> {
                    UserContact newContact = new UserContact();
                    newContact.setOwnerUserId(userId);
                    newContact.setContactPhoneHash(phoneHash);
                    return newContact;
                });

        contact.setContactName(dto.getContactName());
        contact.setStatus(ContactStatus.ACTIVE);

        userContactRepository.save(contact);
    }

    private void softDeleteContact(
            Long userId,
            ContactChangeDto dto
    ) {
        byte[] phoneHash = normalizeAndHash(dto.getPhoneNumber(), null);

        UserContact existing = userContactRepository
                .findByOwnerUserIdAndContactPhoneHash(userId, phoneHash)
                .orElse(null);

        if (existing == null) {
            return;
        }

        existing.setContactName(null);
        existing.setStatus(ContactStatus.DELETED);

        userContactRepository.save(existing);
    }

    private byte[] normalizeAndHash(String phoneNumber, String region) {
        String normalized = PhoneNumberUtils.normalize(phoneNumber, region);
        return contactHashService.hashPhoneNumber(normalized);
    }

    private int safeSize(List<ContactChangeDto> contacts) {
        return contacts == null ? 0 : contacts.size();
    }

    private void markFailed(ContactChangeDto dto, UploadContactsBatchResponse response) {
        response.setSuccess(false);
        response.getErrors().add(
                new ContactUploadErrorDto(dto.getPhoneNumber(), "INVALID_CONTACT")
        );
    }
}