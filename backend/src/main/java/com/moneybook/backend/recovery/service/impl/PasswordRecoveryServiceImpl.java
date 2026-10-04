package com.moneybook.backend.recovery.service.impl;

import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.admin.provider.SystemAdminAuthorizationProvider;
import com.moneybook.backend.common.exception.*;
import com.moneybook.backend.entity.*;
import com.moneybook.backend.enums.*;
import com.moneybook.backend.recovery.*;
import com.moneybook.backend.recovery.dto.*;
import com.moneybook.backend.recovery.repository.*;
import com.moneybook.backend.recovery.service.PasswordRecoveryService;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import com.moneybook.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import com.moneybook.backend.security.ratelimit.RateLimitExceededException;

@Service @RequiredArgsConstructor @Slf4j
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {
    private static final String PASSWORD_RESET="PASSWORD_RESET";
    private static final String DUMMY_ANSWER_HASH=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(UUID.randomUUID().toString());
    private final UserRepository users;
    private final UserAuthRepository auths;
    private final EmailVerificationRepository verifications;
    private final PasswordRecoveryRepository recoveryCodes;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final RecoverySecretGenerator secrets;
    private final EmailSender emailSender;

    @Override public List<SecurityQuestionResponse> questions(){return Arrays.stream(SecurityQuestionCode.values()).map(q->new SecurityQuestionResponse(q,q.question())).toList();}

    @Override @Transactional
    public EmailVerificationResponse requestVerification(String email,String purpose,Authentication authentication){
        String normalized=normalizeEmail(email); Long uid=null;
        if("ACCOUNT_EMAIL".equals(purpose)){
            User user=activeUser(authentication);uid=user.getUserUid();
            if(auths.findLocalByUserUid(uid).isEmpty())throw new BusinessException(ErrorCode.LOCAL_AUTH_NOT_FOUND);
        } else if(!"SIGNUP".equals(purpose)) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        var latest=verifications.findLatestActive(normalized,purpose,uid);
        LocalDateTime now=LocalDateTime.now();
        if(latest.isPresent()&&latest.get().getResendAfter().isAfter(now))
            throw new RateLimitExceededException(60);
        latest.ifPresent(v->{v.invalidate(now);verifications.save(v);});
        String code=secrets.newNumericCode();
        EmailVerification row=verifications.save(new EmailVerification(uid,normalized,purpose,secrets.sha256(code),now.plusMinutes(10),now.plusSeconds(60)));
        emailSender.sendVerificationCode(normalized,code);
        return new EmailVerificationResponse(row.getEmailVerificationUid(),"인증번호를 전송했습니다.");
    }

    @Override @Transactional
    public EmailVerificationResponse requestPasswordResetEmail(String loginId,String email){
        String normalized=normalizeEmail(email);Long uid=auths.findByLocalLoginId(loginId).filter(a->normalized.equals(a.getVerifiedEmail()))
                .map(a->a.getUser().getUserUid()).filter(id->users.findById(id).map(u->u.getStatus()==UserStatus.ACTIVE).orElse(false)).orElse(null);
        LocalDateTime now=LocalDateTime.now();
        var latest=verifications.findLatestActive(normalized,PASSWORD_RESET,uid);
        if(latest.isPresent()&&latest.get().getResendAfter().isAfter(now))
            return new EmailVerificationResponse(latest.get().getEmailVerificationUid(),"입력한 정보와 일치하는 인증 이메일이 있는 경우 인증번호를 전송했습니다.");
        latest.ifPresent(v->{v.invalidate(now);verifications.save(v);});
        String code=secrets.newNumericCode();
        EmailVerification row=verifications.save(new EmailVerification(uid,normalized,PASSWORD_RESET,secrets.sha256(code),now.plusMinutes(10),now.plusSeconds(60)));
        if(uid!=null)try{emailSender.sendVerificationCode(normalized,code);}catch(RuntimeException exception){log.warn("Password recovery email delivery failed cause={}",exception.getClass().getSimpleName());}
        return new EmailVerificationResponse(row.getEmailVerificationUid(),"입력한 정보와 일치하는 인증 이메일이 있는 경우 인증번호를 전송했습니다.");
    }

    @Override @Transactional(noRollbackFor=BusinessException.class)
    public EmailGrantResponse confirmEmail(Long uid,String code){
        EmailVerification row=verifications.findByUidForUpdate(uid).orElseThrow(()->new BusinessException(ErrorCode.EMAIL_VERIFICATION_FAILED));
        LocalDateTime now=LocalDateTime.now();
        if(!row.active(now))throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_FAILED);
        String presented=secrets.sha256(code);
        if(!MessageDigest.isEqual(presented.getBytes(StandardCharsets.US_ASCII),row.getCodeHash().getBytes(StandardCharsets.US_ASCII))){row.failedAttempt();verifications.save(row);throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_FAILED);}
        if(PASSWORD_RESET.equals(row.getPurpose())&&(row.getUserUid()==null||users.findById(row.getUserUid()).filter(u->u.getStatus()==UserStatus.ACTIVE).isEmpty())) {
            row.failedAttempt();verifications.save(row);throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_FAILED);
        }
        String grant=secrets.newGrant();row.issueGrant(secrets.sha256(grant),now.plusMinutes(10));verifications.save(row);
        return new EmailGrantResponse(grant);
    }

    @Override @Transactional
    public void resetByEmail(String token,String password,String confirm){
        validatePassword(password,confirm);
        EmailVerification v=verifications.findActiveGrant(secrets.sha256(token),PASSWORD_RESET)
                .filter(x->x.grantActive(LocalDateTime.now())&&x.getUserUid()!=null)
                .orElseThrow(()->new BusinessException(ErrorCode.PASSWORD_RECOVERY_FAILED));
        User user=users.findById(v.getUserUid()).filter(u->u.getStatus()==UserStatus.ACTIVE).orElseThrow(()->new BusinessException(ErrorCode.PASSWORD_RECOVERY_FAILED));
        UserAuth local=auths.findLocalByUserUidForUpdate(user.getUserUid()).orElseThrow(()->new BusinessException(ErrorCode.PASSWORD_RECOVERY_FAILED));
        v.consumeGrant(LocalDateTime.now());verifications.save(v);completeReset(local,password);
    }

    @Override @Transactional
    public void resetByQuestion(String loginId,String code,String answer,String password,String confirm){
        validatePassword(password,confirm);
        UserAuth local=auths.findByLocalLoginId(loginId).orElse(null);
        boolean questionMatches=local!=null&&local.getSecurityQuestionCode()!=null&&local.getSecurityQuestionCode().equals(code);
        String answerHash=questionMatches?local.getSecurityAnswerHash():null;
        boolean answerMatches=answer!=null&&encoder.matches(answer,answerHash==null?DUMMY_ANSWER_HASH:answerHash);
        if(local==null||!active(local)||!questionMatches||answerHash==null||!answerMatches)
            throw new BusinessException(ErrorCode.PASSWORD_RECOVERY_FAILED);
        completeReset(local,password);
    }

    @Override @Transactional
    public void resetByRecoveryCode(String loginId,String recoveryCode,String password,String confirm){
        validatePassword(password,confirm);UserAuth local=auths.findByLocalLoginId(loginId).orElse(null);
        if(local==null||!active(local)||recoveryCode==null)throw new BusinessException(ErrorCode.PASSWORD_RECOVERY_FAILED);
        String digest=secrets.sha256(recoveryCode.toUpperCase(Locale.ROOT));
        PasswordRecoveryCode row=recoveryCodes.findUsableByHash(digest).filter(c->c.getUserUid().equals(local.getUser().getUserUid()))
                .orElseThrow(()->new BusinessException(ErrorCode.PASSWORD_RECOVERY_FAILED));
        row.use(LocalDateTime.now());recoveryCodes.save(row);completeReset(local,password);
    }

    @Override @Transactional(readOnly=true)
    public AccountSecurityResponse accountSecurity(Authentication authentication){User user=activeUser(authentication);UserAuth local=auths.findLocalByUserUid(user.getUserUid()).orElseThrow(()->new BusinessException(ErrorCode.LOCAL_AUTH_NOT_FOUND));
        int remaining=(int)recoveryCodes.findByUserUid(user.getUserUid()).stream().filter(PasswordRecoveryCode::usable).count();String email=local.getVerifiedEmail();
        return new AccountSecurityResponse(local.getSecurityQuestionCode()!=null,local.getSecurityQuestionCode()==null?null:SecurityQuestionCode.valueOf(local.getSecurityQuestionCode()),remaining,email!=null,maskEmail(email),local.isPasswordChangeRequired());}

    @Override @Transactional
    public void updateQuestion(Authentication authentication,SecurityQuestionUpdateRequest request){UserAuth local=requireLocal(authentication);checkCurrent(local,request.currentPassword());
        if(!request.answer().equals(request.answer().strip())||request.answer().getBytes(StandardCharsets.UTF_8).length>72)throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        local.changeSecurityQuestion(request.questionCode().name(),encoder.encode(request.answer()));auths.save(local);}

    @Override @Transactional
    public RecoveryCodesResponse regenerateCodes(Authentication authentication,String currentPassword){UserAuth local=requireLocal(authentication);checkCurrent(local,currentPassword);LocalDateTime now=LocalDateTime.now();
        recoveryCodes.findByUserUid(local.getUser().getUserUid()).forEach(c->c.revoke(now));var codes=secrets.newRecoveryCodes(8);codes.forEach(c->recoveryCodes.save(new PasswordRecoveryCode(local.getUser().getUserUid(),secrets.sha256(c))));
        return new RecoveryCodesResponse(codes,"기존 복구코드는 다시 확인할 수 없습니다.");}

    @Override @Transactional
    public void applyEmail(Authentication authentication,String token){UserAuth local=requireLocal(authentication);EmailVerification v=verifications.findActiveGrant(secrets.sha256(token),"ACCOUNT_EMAIL")
            .filter(x->Objects.equals(x.getUserUid(),local.getUser().getUserUid())&&x.grantActive(LocalDateTime.now())).orElseThrow(()->new BusinessException(ErrorCode.EMAIL_VERIFICATION_FAILED));
        v.consumeGrant(LocalDateTime.now());local.changeVerifiedEmail(v.getEmail(),LocalDateTime.now());try{auths.save(local);verifications.save(v);}catch(DataIntegrityViolationException e){throw new BusinessException(ErrorCode.EMAIL_ALREADY_IN_USE);}}

    @Override @Transactional
    public void removeEmail(Authentication authentication,String currentPassword){UserAuth local=requireLocal(authentication);checkCurrent(local,currentPassword);
        boolean hasRecoveryMethod=local.getSecurityQuestionCode()!=null||recoveryCodes.findByUserUid(local.getUser().getUserUid()).stream().anyMatch(PasswordRecoveryCode::usable);
        if(!hasRecoveryMethod)throw new BusinessException(ErrorCode.PASSWORD_RECOVERY_METHOD_REQUIRED);
        local.changeVerifiedEmail(null,null);auths.save(local);}

    private void completeReset(UserAuth local,String password){local.completePasswordChange(encoder.encode(password));auths.save(local);LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);sessions.findUnrevokedByUserUid(local.getUser().getUserUid()).forEach(s->s.revoke(now,"PASSWORD_RECOVERY"));}
    private void validatePassword(String password,String confirm){if(password==null||!password.equals(confirm))throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);if(password.getBytes(StandardCharsets.UTF_8).length>72)throw new BusinessException(ErrorCode.INVALID_PASSWORD_LENGTH);if(password.isBlank())throw new BusinessException(ErrorCode.VALIDATION_FAILED);}
    private boolean active(UserAuth a){return a.getUser().getStatus()==UserStatus.ACTIVE;}
    private UserAuth requireLocal(Authentication a){User u=activeUser(a);return auths.findLocalByUserUidForUpdate(u.getUserUid()).orElseThrow(()->new BusinessException(ErrorCode.LOCAL_AUTH_NOT_FOUND));}
    private User activeUser(Authentication a){if(!(a instanceof JwtAuthenticationToken))throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);Long uid;try{uid=Long.valueOf(a.getName());}catch(RuntimeException e){throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);}return users.findById(uid).filter(u->u.getStatus()==UserStatus.ACTIVE).orElseThrow(()->new BusinessException(ErrorCode.USER_INACTIVE));}
    private void checkCurrent(UserAuth a,String password){if(!encoder.matches(password,a.getPasswordHash()))throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);}
    private String normalizeEmail(String email){if(email==null||email.isBlank()||email.length()>254)throw new BusinessException(ErrorCode.VALIDATION_FAILED);return email.strip().toLowerCase(Locale.ROOT);}
    private String maskEmail(String email){if(email==null)return null;int at=email.indexOf('@');if(at<1)return "***";return email.substring(0,1)+"***"+email.substring(at);}
}
