package com.moni.user.admin.presentation.dto.request;

import com.moni.user.user.domain.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoleChangeRequest {

    @NotNull(message = "변경할 권한은 필수입니다.")
    private UserRole role;
}
