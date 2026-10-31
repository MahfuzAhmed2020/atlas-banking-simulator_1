package com.atlas.banking;

import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Tag(name="Mobile Check Deposits")
@RestController @RequestMapping("/api/customer/checks") @PreAuthorize("hasRole('CUSTOMER')")
class DepositController {
 private final DepositService service;private final Checks checks;private final Policy policy;
 DepositController(DepositService s,Checks c,Policy p){service=s;checks=c;policy=p;}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 CheckDeposit deposit(@RequestParam Long accountId,@RequestParam String checkNumber,@RequestParam BigDecimal amount,
   @RequestParam boolean instant,@RequestPart MultipartFile front,@RequestPart MultipartFile back,Authentication auth) throws IOException{
  BankUser user=policy.user(auth.getName());policy.own(auth.getName(),accountId);
  return service.submit(user.id,accountId,checkNumber,amount,instant,front,back);
 }
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping List<CheckDeposit> list(Authentication auth){return checks.findByUserIdOrderByIdDesc(policy.user(auth.getName()).id);}
 @Operation(description="Uses the application session and enforces route and resource permissions.")
 @ApiResponse(responseCode="200", description="Operation completed")
 @GetMapping("/{id}/{side}") ResponseEntity<byte[]> image(@PathVariable Long id,@PathVariable String side,Authentication auth){
  if(!List.of("front","back").contains(side))throw new IllegalArgumentException("Invalid side");
  byte[] bytes=service.readOwned(policy.user(auth.getName()).id,id,side);
  MediaType content=(bytes[0]&255)==137?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG;
  return ResponseEntity.ok().contentType(content).cacheControl(CacheControl.noStore()).body(bytes);
 }
}
