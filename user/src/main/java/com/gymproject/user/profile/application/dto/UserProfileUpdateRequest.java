package com.gymproject.user.profile.application.dto;

import com.gymproject.user.profile.exception.UserErrorCode;
import com.gymproject.user.profile.exception.UserException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "사용자 프로필 수정 요청 DTO")
public class UserProfileUpdateRequest {

    @Schema(description = "수정할 이름(이름)", example = "gildong")
    private String firstName;

    @Schema(description = "수정할 이름(성)", example = "hong")
    private String lastName;

    @Schema(description = "수정할 휴대폰 번호 (호주 번호 포맷: 04xx xxx xxx 또는 +61 4xx xxx xxx)",
            example = "0412 345 678")
    @Pattern(regexp = "^(?:\\+61[-\\s]?4\\d{2}[-\\s]?\\d{3}[-\\s]?\\d{3}|04\\d{2}[-\\s]?\\d{3}[-\\s]?\\d{3})$",
            message = "Please enter a valid Australian mobile number (e.g. 0412 345 678 or +61 412 345 678).")
    private String phoneNumber;

    public void validateAllBlank() {
        if (firstName.isBlank()
                && lastName.isBlank()
                && phoneNumber.isBlank()) {
            throw new UserException(UserErrorCode.INVALID_FORMAT);
        }
    }

}
