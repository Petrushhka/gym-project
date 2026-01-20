package com.gymproject.auth.infrastructure.jwt;

import com.gymproject.common.dto.auth.UserAuthInfo;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

   private final TokenProvider tokenProvider;

   private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // [디버깅 1] 요청 URL과 헤더 확인
        String requestURI = request.getRequestURI();
        String header = request.getHeader("Authorization");
        System.out.println(">>> 요청 URL: " + requestURI);
        System.out.println(">>> 헤더 값(Authorization): " + header); // 여기가 null이면 포스트맨 문제!

        String token = parseBearerToken(request);

        try{
            if(token != null){
                UserAuthInfo userInfo = tokenProvider.validateAndGetUserAuthInfo(token);
                log.debug("Authenticated user: {}, Role: {}", userInfo.getEmail(), userInfo.getRole());
                System.out.println("userInfo: "+userInfo);

                List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
                authorityList.add(new SimpleGrantedAuthority("ROLE_"+userInfo.getRole().name()));

                // SecurityContext에 인증정보를 넣음.
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userInfo, null, authorityList);
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                System.out.println(">>> 토큰 검증 성공 & SecurityContext 저장 완료");
            }else{
                System.out.println(">>> 토큰이 null이라서 인증 로직 건너뜀");
            }
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage()); // 토큰 만료
            request.setAttribute("exception", "EXPIRED_TOKEN");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage()); // 서명 오류 또는 토큰 형식 오류
            request.setAttribute("exception", "INVALID_TOKEN");
        } catch (Exception e) {
            //  토큰 검증 외의 알 수 없는 서버 오류
            log.error("JWT filter internal error: {}", e.getMessage(), e);
            request.setAttribute("exception", "INTERNAL_ERROR");
        }

        filterChain.doFilter(request, response); // 이거 없으면 요청 처리가 되지가 않음
    }

    private String parseBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }

        return null;
    }
}

/*

 }catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(e.getMessage());
        }

        filterChain.doFilter(request, response); // 이거 없으면 요청 처리가 되지가 않음
    }

  1)  기존에 Catch문에 retrun 문이 없어서 그 밑에 filterChain이 실행되는 문제가 있었음.


   2) JwtAuthFilter는 지금 2 가지의 책임을 동시에 지니고 있음(SRP 단일책임 원칙 위반)

   401에러 응답 등은 예외처리를 담당하는 곳에 맡겨야함.

     2-1) Jwtfilter에서 만든 토큰이 유효하면 -> SecurityConext에 인증정보를 넣음
     2-2) 토큰이 유효하지 않으면 다음 필터로 넘어감
     2-3) 뒤이은 필터에서 SecurityContext가 비어있음을 인지하면 인증이 이루어지지 않음을 확인할 수 있음
     2-4) AuthenticationEntryPoin에서 인증이 되지 않았다고 401 Unauthorized 응답을 만들어서 보냄







 */



