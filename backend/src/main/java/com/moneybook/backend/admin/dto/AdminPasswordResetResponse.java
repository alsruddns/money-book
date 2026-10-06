package com.moneybook.backend.admin.dto;
public record AdminPasswordResetResponse(Long userUid,String temporaryPassword,boolean passwordChangeRequired){
 @Override public String toString(){return "AdminPasswordResetResponse[userUid="+userUid+", temporaryPassword=REDACTED, passwordChangeRequired="+passwordChangeRequired+"]";}
}
