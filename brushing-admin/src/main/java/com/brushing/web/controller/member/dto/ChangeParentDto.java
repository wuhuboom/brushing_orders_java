package com.brushing.web.controller.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Minimal request used to move a member branch to another parent.
 */
public class ChangeParentDto {

    @NotNull(message = "{member.user.id.required}")
    private Long memberId;

    @NotBlank(message = "{member.user.parent_identifier.required}")
    private String parentIdentifier;

    @NotNull(message = "{member.user.version.required}")
    private Long version;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getParentIdentifier() {
        return parentIdentifier;
    }

    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
