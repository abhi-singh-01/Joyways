package in.joyway.controller;

import in.joyway.dto.ApiResponse;
import in.joyway.dto.contact.ContactEnquiryRequest;
import in.joyway.service.ContactEnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactEnquiryService contactEnquiryService;

    @PostMapping("/enquiry")
    public ApiResponse<Void> submitEnquiry(@Valid @RequestBody ContactEnquiryRequest request) {
        contactEnquiryService.submitEnquiry(request);
        return ApiResponse.success(null, "Thank you. We received your message and will respond shortly.");
    }
}
