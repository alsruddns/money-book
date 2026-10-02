package com.moneybook.backend.admin.dto;

import com.moneybook.backend.enums.SystemRole;
import jakarta.validation.constraints.NotNull;

public record ChangeSystemRoleRequest(@NotNull SystemRole systemRole) { }
