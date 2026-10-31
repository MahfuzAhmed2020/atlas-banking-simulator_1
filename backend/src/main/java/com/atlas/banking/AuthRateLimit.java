package com.atlas.banking;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded, local-instance throttle for the simulator's public credential/code endpoints. */
class AuthRateLimit extends OncePerRequestFilter {
 record Bucket(long window,int count){}
 final ConcurrentHashMap<String,Bucket> buckets=new ConcurrentHashMap<>();
 @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  String path=r.getRequestURI();
  if(!"POST".equals(r.getMethod())||!path.startsWith("/api/auth/")||path.endsWith("/logout")){chain.doFilter(r,response);return;}
  long minute=System.currentTimeMillis()/60000;
  if(buckets.size()>10000)buckets.entrySet().removeIf(e->e.getValue().window()<minute);
  if(buckets.size()>20000){response.sendError(429,"Authentication temporarily busy");return;}
  // Trust the direct connection address, not a user-supplied forwarding header.
  String key=r.getRemoteAddr()+":"+path;
  Bucket bucket=buckets.compute(key,(k,old)->old==null||old.window()!=minute?new Bucket(minute,1):new Bucket(minute,old.count()+1));
  if(bucket.count()>15){response.setStatus(429);response.setHeader("Retry-After","60");response.setContentType("application/json");response.getWriter().write("{\"error\":\"Too many authentication attempts; retry in one minute\"}");return;}
  chain.doFilter(r,response);
 }
}
