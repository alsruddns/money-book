package com.moneybook.backend.recovery.dto;
import java.util.List;
public record RecoveryCodesResponse(List<String> recoveryCodes,String message){
 @Override public String toString(){return "RecoveryCodesResponse[recoveryCodes=REDACTED, message="+message+"]";}
}
