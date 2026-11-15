package com.gawt.gawt_backend.dto;

import com.gawt.gawt_backend.model.Condition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ItemRequest(
                @NotBlank(message = "Title is required") String title,

                String description,

                String category,

                @NotNull(message = "Condition is required") Condition condition,

                String meetupLocation,

                MultipartFile thumbnail,

                MultipartFile[] additionalImages) {
}
