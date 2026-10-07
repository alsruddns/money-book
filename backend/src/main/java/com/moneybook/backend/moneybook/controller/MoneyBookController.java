package com.moneybook.backend.moneybook.controller;

import com.moneybook.backend.moneybook.dto.CreateMoneyBookRequest;
import com.moneybook.backend.moneybook.dto.CreateMoneyBookResponse;
import com.moneybook.backend.moneybook.dto.CreateInvitationRequest;
import com.moneybook.backend.moneybook.dto.InvitationResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookListResponse;
import com.moneybook.backend.moneybook.dto.PendingInvitationResponse;
import com.moneybook.backend.moneybook.dto.MoneyBookMemberResponse;
import com.moneybook.backend.moneybook.dto.UpdateMoneyBookMemberPermissionRequest;
import com.moneybook.backend.moneybook.dto.TransferMoneyBookOwnerRequest;
import com.moneybook.backend.moneybook.dto.MoneyBookOwnerTransferResponse;
import com.moneybook.backend.moneybook.service.MoneyBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Access Token으로 인증된 사용자의 가계부 생성, 목록 및 사용자 초대 API를 제공한다.
 */
@RestController
@RequestMapping("/money-books")
@RequiredArgsConstructor
public class MoneyBookController {

    private final MoneyBookService moneyBookService;

    /**
     * 현재 사용자를 소유자와 전체 권한을 가진 수락 멤버로 등록하며 가계부를 생성한다.
     *
     * @param request 필수 가계부 이름(최대 100자)
     * @param authentication 검증된 Access Token의 인증 정보
     * @return 생성된 가계부 UID, 이름과 소유자 UID
     */
    @PostMapping
    public ResponseEntity<CreateMoneyBookResponse> create(
            @Valid @RequestBody CreateMoneyBookRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(moneyBookService.create(request, authentication));
    }

    /**
     * 현재 사용자의 수락된 멤버십 중 읽기 권한이 있는 가계부를 최신 생성 순으로 조회한다.
     * 결과가 없으면 빈 배열을 반환한다.
     *
     * @param authentication 검증된 Access Token의 인증 정보
     * @return 접근 가능한 가계부와 각 멤버십의 권한
     */
    @GetMapping
    public ResponseEntity<List<MoneyBookListResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(moneyBookService.list(authentication));
    }

    /**
     * 가계부 owner 또는 수락된 관리자 멤버가 기존 LOCAL 사용자를 초대한다.
     * 거절된 초대는 같은 멤버십을 재사용하며 관리자 권한을 주면 모든 일반 권한도 부여한다.
     *
     * @param moneyBookUid 초대할 가계부 UID
     * @param request 대상 loginId와 초기 O/C/R/U/D 권한
     * @param authentication 검증된 Access Token의 인증 정보
     * @return 생성되거나 다시 대기 상태가 된 초대 정보
     */
    @PostMapping("/{moneyBookUid}/invitations")
    public ResponseEntity<InvitationResponse> invite(
            @PathVariable Long moneyBookUid, @Valid @RequestBody CreateInvitationRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moneyBookService.invite(moneyBookUid, request, authentication));
    }

    /**
     * 현재 사용자가 받은 대기 중 초대를 읽기 권한과 관계없이 조회한다.
     *
     * @param authentication 검증된 Access Token의 인증 정보
     * @return 대기 중 초대 목록, 없으면 빈 배열
     */
    @GetMapping("/invitations")
    public ResponseEntity<List<PendingInvitationResponse>> pendingInvitations(Authentication authentication) {
        return ResponseEntity.ok(moneyBookService.pendingInvitations(authentication));
    }

    /**
     * 현재 사용자의 해당 가계부 PENDING 초대를 수락한다.
     *
     * @param moneyBookUid 초대가 속한 가계부 UID
     * @param moneyBookUserUid 처리할 멤버십 UID
     * @param authentication 검증된 Access Token의 인증 정보
     * @return ACCEPTED 상태의 멤버십 정보
     */
    @PatchMapping("/{moneyBookUid}/invitations/{moneyBookUserUid}/accept")
    public ResponseEntity<InvitationResponse> acceptInvitation(
            @PathVariable Long moneyBookUid, @PathVariable Long moneyBookUserUid,
            Authentication authentication) {
        return ResponseEntity.ok(moneyBookService.acceptInvitation(
                moneyBookUid, moneyBookUserUid, authentication));
    }

    /**
     * 현재 사용자의 해당 가계부 PENDING 초대를 거절한다.
     *
     * @param moneyBookUid 초대가 속한 가계부 UID
     * @param moneyBookUserUid 처리할 멤버십 UID
     * @param authentication 검증된 Access Token의 인증 정보
     * @return REJECTED 상태의 멤버십 정보
     */
    @PatchMapping("/{moneyBookUid}/invitations/{moneyBookUserUid}/reject")
    public ResponseEntity<InvitationResponse> rejectInvitation(
            @PathVariable Long moneyBookUid, @PathVariable Long moneyBookUserUid,
            Authentication authentication) {
        return ResponseEntity.ok(moneyBookService.rejectInvitation(
                moneyBookUid, moneyBookUserUid, authentication));
    }

    /**
     * 가계부의 가입 완료 멤버를 owner 우선으로 조회한다.
     * owner 또는 읽기 권한이 있는 ACCEPTED 멤버만 조회할 수 있다.
     */
    @GetMapping("/{moneyBookUid}/members")
    public ResponseEntity<List<MoneyBookMemberResponse>> members(
            @PathVariable Long moneyBookUid, Authentication authentication) {
        return ResponseEntity.ok(moneyBookService.members(moneyBookUid, authentication));
    }

    /**
     * owner 또는 가입 완료 관리자가 일반 멤버의 O/C/R/U/D 권한을 변경한다.
     * owner의 권한은 변경할 수 없으며 O 권한이 있으면 C/R/U/D가 모두 부여된다.
     */
    @PatchMapping("/{moneyBookUid}/members/{moneyBookUserUid}/permissions")
    public ResponseEntity<Void> updateMemberPermissions(
            @PathVariable Long moneyBookUid, @PathVariable Long moneyBookUserUid,
            @Valid @RequestBody UpdateMoneyBookMemberPermissionRequest request,
            Authentication authentication) {
        moneyBookService.updateMemberPermissions(moneyBookUid, moneyBookUserUid, request, authentication);
        return ResponseEntity.noContent().build();
    }

    /**
     * owner 또는 가입 완료 관리자가 일반 멤버의 멤버십을 제거한다.
     * 가계부의 원래 owner는 제거할 수 없다.
     */
    @DeleteMapping("/{moneyBookUid}/members/{moneyBookUserUid}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long moneyBookUid, @PathVariable Long moneyBookUserUid,
            Authentication authentication) {
        moneyBookService.removeMember(moneyBookUid, moneyBookUserUid, authentication);
        return ResponseEntity.noContent().build();
    }

    /**
     * 현재 OWNER만 소유권을 이전할 수 있으며, 대상은 ACTIVE 상태의 ACCEPTED 멤버여야 한다.
     * 대상 멤버에게 O/C/R/U/D 전체 권한을 보장하고 기존 OWNER membership은 유지한다.
     */
    @PatchMapping("/{moneyBookUid}/owner")
    public ResponseEntity<MoneyBookOwnerTransferResponse> transferOwner(
            @PathVariable Long moneyBookUid,
            @Valid @RequestBody TransferMoneyBookOwnerRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(moneyBookService.transferOwner(moneyBookUid, request, authentication));
    }
}
