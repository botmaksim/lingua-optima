package com.linguaoptima.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddStudentRequest {

    @NotBlank(message = "Student email is required")
    @Email(message = "Invalid email format")
    private String email;
}
