package com.atlas.banking;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AuthenticatorRulesTest {
 // RFC 6238 SHA-1 test secret; six-digit truncations of the published eight-digit vectors.
 static final String SECRET="GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
 @Test void matchesRfc6238Sha1Vector(){assertEquals("287082",AuthenticatorService.code(SECRET,59/30));assertEquals("081804",AuthenticatorService.code(SECRET,1111111109L/30));assertEquals("050471",AuthenticatorService.code(SECRET,1111111111L/30));}
 @Test void onlyAcceptsAdjacentTimeWindows(){assertEquals(1,AuthenticatorService.match(SECRET,"287082",59));assertEquals(-1,AuthenticatorService.match(SECRET,"287082",180));}
 @Test void rejectsMalformedCodes(){assertEquals(-1,AuthenticatorService.match(SECRET,"",59));assertEquals(-1,AuthenticatorService.match(SECRET,null,59));assertEquals(-1,AuthenticatorService.match(SECRET,"abcdef",59));}
 @Test void secretsContain160Bits(){String s=AuthenticatorService.secret();assertEquals(32,s.length());assertEquals(20,AuthenticatorService.decode(s).length);assertNotEquals(s,AuthenticatorService.secret());}
}
