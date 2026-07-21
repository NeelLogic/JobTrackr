package solannee.sheridancollege.ca.jobtrackr.security;
import jakarta.servlet.*; import jakarta.servlet.http.*; import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.security.core.userdetails.*; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter; import java.io.IOException; import io.jsonwebtoken.JwtException;
@Component public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtService jwtService; private final UserDetailsService users;
 public JwtAuthenticationFilter(JwtService jwtService,UserDetailsService users){this.jwtService=jwtService;this.users=users;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String header=req.getHeader("Authorization"); if(header!=null&&header.startsWith("Bearer ")&&SecurityContextHolder.getContext().getAuthentication()==null){
   try{String email=jwtService.extractSubject(header.substring(7));UserDetails user=users.loadUserByUsername(email);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities()));}
   catch(JwtException|UsernameNotFoundException ignored){SecurityContextHolder.clearContext();}
  } chain.doFilter(req,res);
 }
}
