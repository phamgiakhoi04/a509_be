package com.A509.DTO;
import lombok.Data;

@Data
public class RegisterDTO {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="[a-zA-Z0-9_.-]{3,64}")
    private String username;
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Email
    @jakarta.validation.constraints.Size(max=254)
    private String email;
    private String password;
    @jakarta.validation.constraints.Size(max=150)
    private String fullName;
    @jakarta.validation.constraints.Size(max=30)
    private String phoneNumber;
}