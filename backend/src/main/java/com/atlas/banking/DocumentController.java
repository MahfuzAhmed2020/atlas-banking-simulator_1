package com.atlas.banking;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.*;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/documents") @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','ADMIN')")
class DocumentController {
 final DocumentService service;
 DocumentController(DocumentService s){service=s;}
 @GetMapping List<BankDocument> list(@RequestParam(required=false) Long customerId,Authentication a){BankUser u=service.policy().user(a.getName());Long id=customerId==null?u.id:customerId;service.access(a.getName(),id);return service.documents().findByUserIdOrderByIdDesc(id);}
 record Generate(Long accountId,String kind,LocalDate start,LocalDate end){}
 @PostMapping BankDocument generate(@RequestBody Generate r,Authentication a){return service.generate(a.getName(),r.accountId(),r.kind(),r.start(),r.end());}
 record Receipt(String kind,Long sourceId){}
 @PostMapping("/receipts") BankDocument receipt(@RequestBody Receipt r,Authentication a){return service.receipt(a.getName(),r.kind(),r.sourceId());}
 @GetMapping(value="/{id}/download",produces="application/pdf") ResponseEntity<byte[]> download(@PathVariable Long id,Authentication a){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=atlas-document-"+id+".pdf").body(service.download(a.getName(),id));}
 @PostMapping("/{id}/email") DocumentDelivery email(@PathVariable Long id,Authentication a){return service.email(a.getName(),id);}
 @GetMapping("/{id}/deliveries") List<DocumentDelivery> deliveries(@PathVariable Long id,Authentication a){BankDocument d=service.documents().findById(id).orElseThrow(()->new IllegalArgumentException("Document not found"));service.access(a.getName(),d.userId);return service.deliveries().findByDocumentIdOrderByIdDesc(id);}
}
