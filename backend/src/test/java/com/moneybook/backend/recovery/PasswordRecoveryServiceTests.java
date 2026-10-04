package com.moneybook.backend.recovery;

import com.moneybook.backend.auth.repository.UserAuthRepository;
import com.moneybook.backend.common.exception.BusinessException;
import com.moneybook.backend.common.exception.ErrorCode;
import com.moneybook.backend.entity.PasswordRecoveryCode;
import com.moneybook.backend.entity.User;
import com.moneybook.backend.entity.UserAuth;
import com.moneybook.backend.enums.SecurityQuestionCode;
import com.moneybook.backend.enums.UserStatus;
import com.moneybook.backend.recovery.repository.EmailVerificationRepository;
import com.moneybook.backend.recovery.repository.PasswordRecoveryRepository;
import com.moneybook.backend.recovery.service.impl.PasswordRecoveryServiceImpl;
import com.moneybook.backend.session.repository.RefreshSessionRepository;
import com.moneybook.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.moneybook.backend.entity.EmailVerification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordRecoveryServiceTests {
 private final UserRepository users=mock(UserRepository.class);
 private final UserAuthRepository auths=mock(UserAuthRepository.class);
 private final EmailVerificationRepository verifications=mock(EmailVerificationRepository.class);
 private final PasswordRecoveryRepository codes=mock(PasswordRecoveryRepository.class);
 private final RefreshSessionRepository sessions=mock(RefreshSessionRepository.class);
 private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
 private final RecoverySecretGenerator secrets=new RecoverySecretGenerator();
 private final PasswordRecoveryServiceImpl service=new PasswordRecoveryServiceImpl(users,auths,verifications,codes,sessions,encoder,secrets,mock(EmailSender.class));
 private User user; private UserAuth local;

 @BeforeEach void setUp(){user=mock(User.class);when(user.getUserUid()).thenReturn(9L);when(user.getStatus()).thenReturn(UserStatus.ACTIVE);local=UserAuth.local(user,"member",encoder.encode("old-password"));local.changeSecurityQuestion(SecurityQuestionCode.FAVORITE_FOOD.name(),encoder.encode("제육볶음"));when(auths.findByLocalLoginId("member")).thenReturn(Optional.of(local));when(sessions.findUnrevokedByUserUid(any())).thenReturn(List.of());}

 @Test void exactSecurityAnswerResetsPasswordAndRevokesAllRefreshSessions(){
  var session=com.moneybook.backend.entity.RefreshTokenSession.create(9L,"session","hash",null,null,
          java.time.LocalDateTime.now().minusMinutes(1),java.time.LocalDateTime.now().plusDays(1));
  when(sessions.findUnrevokedByUserUid(9L)).thenReturn(List.of(session));
  service.resetByQuestion("member",SecurityQuestionCode.FAVORITE_FOOD.name(),"제육볶음","new-password","new-password");
  assertTrue(encoder.matches("new-password",local.getPasswordHash()));
  assertNotNull(session.getRevokedAt());verify(sessions).findUnrevokedByUserUid(9L);
 }

 @Test void securityAnswerWhitespaceAndCaseMustMatchExactly(){
  String original=local.getPasswordHash();
  assertEquals(ErrorCode.PASSWORD_RECOVERY_FAILED,assertThrows(BusinessException.class,()->service.resetByQuestion("member","FAVORITE_FOOD","제육 볶음","new-password","new-password")).getErrorCode());
  assertEquals(ErrorCode.PASSWORD_RECOVERY_FAILED,assertThrows(BusinessException.class,()->service.resetByQuestion("member","FAVORITE_FOOD","제육볶음 ","new-password","new-password")).getErrorCode());
  local.changeSecurityQuestion(SecurityQuestionCode.FAVORITE_WORK.name(),encoder.encode("Blue Moon"));
  assertEquals(ErrorCode.PASSWORD_RECOVERY_FAILED,assertThrows(BusinessException.class,()->service.resetByQuestion("member","FAVORITE_WORK","blue moon","new-password","new-password")).getErrorCode());
  assertEquals(original,local.getPasswordHash());verifyNoInteractions(sessions);
 }

 @Test void recoveryCodeIsOneTimeAndOnlyItsDigestIsUsedForLookup(){
  String raw="ABCD-EFGH-JKLM-NPQR-STUV";
  PasswordRecoveryCode stored=new PasswordRecoveryCode(9L,secrets.sha256(raw));
  var session=com.moneybook.backend.entity.RefreshTokenSession.create(9L,"session","hash",null,null,
          java.time.LocalDateTime.now().minusMinutes(1),java.time.LocalDateTime.now().plusDays(1));
  when(sessions.findUnrevokedByUserUid(9L)).thenReturn(List.of(session));
  when(auths.findByLocalLoginId("member")).thenReturn(Optional.of(local));
  when(codes.findUsableByHash(secrets.sha256(raw))).thenReturn(Optional.of(stored));
  service.resetByRecoveryCode("member",raw,"new-password","new-password");
  assertNotNull(stored.getUsedAt());assertTrue(encoder.matches("new-password",local.getPasswordHash()));assertNotNull(session.getRevokedAt());
  verify(codes).findUsableByHash(secrets.sha256(raw));
 }

 @Test void generatedRecoveryCodesHaveHighEntropyOneTimePresentationFormat(){
  var generated=secrets.newRecoveryCodes(8);
  assertEquals(8,generated.size());assertEquals(8,generated.stream().distinct().count());
  assertTrue(generated.stream().allMatch(code->code.matches("[A-HJ-NP-Z2-9]{4}(-[A-HJ-NP-Z2-9]{4}){4}")));
  assertNotEquals(generated.get(0),secrets.sha256(generated.get(0)));
 }

 @Test void emailRecoveryUsesGenericResponseAndSendsOnlyForMatchingVerifiedIdentity(){
  var mail=mock(EmailSender.class);var emailSecrets=new RecoverySecretGenerator();
  var emailService=new PasswordRecoveryServiceImpl(users,auths,verifications,codes,sessions,encoder,emailSecrets,mail);
  when(verifications.findLatestActive("member@example.com","PASSWORD_RESET",null)).thenReturn(Optional.empty());
  when(verifications.save(any(EmailVerification.class))).thenAnswer(invocation->invocation.getArgument(0));
  var missing=emailService.requestPasswordResetEmail("missing","member@example.com");
  verifyNoInteractions(mail);

  User emailOwner=mock(User.class);when(emailOwner.getUserUid()).thenReturn(77L);when(emailOwner.getStatus()).thenReturn(UserStatus.ACTIVE);
  UserAuth verified=UserAuth.local(emailOwner,"member",encoder.encode("old-password"));verified.changeVerifiedEmail("member@example.com",java.time.LocalDateTime.now());
  when(auths.findByLocalLoginId("member")).thenReturn(Optional.of(verified));when(users.findById(77L)).thenReturn(Optional.of(emailOwner));
  when(verifications.findLatestActive("member@example.com","PASSWORD_RESET",77L)).thenReturn(Optional.empty());
  var matching=emailService.requestPasswordResetEmail("member","member@example.com");
  assertEquals(missing.message(),matching.message());
  ArgumentCaptor<String> sentCode=ArgumentCaptor.forClass(String.class);verify(mail).sendVerificationCode(org.mockito.ArgumentMatchers.eq("member@example.com"),sentCode.capture());
  ArgumentCaptor<EmailVerification> row=ArgumentCaptor.forClass(EmailVerification.class);verify(verifications,times(2)).save(row.capture());
  assertEquals(emailSecrets.sha256(sentCode.getValue()),row.getAllValues().get(1).getCodeHash());
  assertNotEquals(sentCode.getValue(),row.getAllValues().get(1).getCodeHash());
 }
}
