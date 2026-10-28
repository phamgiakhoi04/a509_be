package com.A509.DTO;
import lombok.Data;

@Data
public class LoginDTO {
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max=64)
    private String username;
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max=200)
    private String password;
}