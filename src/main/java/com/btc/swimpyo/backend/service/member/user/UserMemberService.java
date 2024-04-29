package com.btc.swimpyo.backend.service.member.user;

import com.btc.swimpyo.backend.config.Constant;
import com.btc.swimpyo.backend.dto.member.user.UserMemberDto;
import com.btc.swimpyo.backend.mappers.member.user.IUserMemberDaoMapper;
import com.btc.swimpyo.backend.utils.jwt.entity.RefTokenEntity;
import com.btc.swimpyo.backend.utils.jwt.filter.JwtAuthenticationFilter;
import com.btc.swimpyo.backend.utils.jwt.provider.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserMemberService implements IUserMemberService {

    @Value("${secret-key}")
    private String secretKey;

    private final IUserMemberDaoMapper iUserMemberDaoMapper;
    private final JwtProvider jwtProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public int signUp(Map<String, Object> msgMap, UserMemberDto userMemberDto) {
        log.info("signUp");
        String birthDate = msgMap.get("birth").toString().split("T")[0];
        userMemberDto.setU_m_email(msgMap.get("email").toString());
        userMemberDto.setU_m_pw(passwordEncoder.encode(msgMap.get("pw").toString()));
        userMemberDto.setU_m_name(msgMap.get("name").toString());
        userMemberDto.setU_m_birth(birthDate);
        userMemberDto.setU_m_phone(msgMap.get("phone").toString());
        userMemberDto.setU_m_nickname(msgMap.get("nickname").toString());

        int result = Constant.USER_SIGNUP_FAIL;
        UserMemberDto idVerifiedUserMemberDto = iUserMemberDaoMapper.isMemberUser(userMemberDto);
        if (idVerifiedUserMemberDto != null) {
            return result;
        }

        result = iUserMemberDaoMapper.insertMemberUser(userMemberDto);
        if (result > 0) {
            log.info("SIGN UP SUCCESS");
            result = Constant.USER_SIGNUP_SUCCESS;
        } else {
            log.info("SIGN UP FAIL");
            result = Constant.USER_DUP_MEMBER;
        }
        return result;
    }

    @Override
    public Map<String, Object> signIn(Map<String, Object> msgMap,
                                      UserMemberDto userMemberDto,
                                      RefTokenEntity refTokenEntity,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        log.info("signIn");

        userMemberDto.setU_m_email(msgMap.get("email").toString());
        userMemberDto.setU_m_pw(msgMap.get("pw").toString());

        Map<String, Object> map = new HashMap<>();

        // 동일한 username 없음
        UserMemberDto idVerifiedUserMemberDto = iUserMemberDaoMapper.isMemberUser(userMemberDto);

        if (idVerifiedUserMemberDto != null && passwordEncoder.matches(userMemberDto.getU_m_pw(), idVerifiedUserMemberDto.getU_m_pw())) {
            /*
                로그인 시
                동일한 refresh token 명의 행이 있다면 delete 후
                새로 발급받은 refresh token을 insert해준다.
             */
            String checkingRefToken = null;
            Cookie[] authHeader = request.getCookies();
            if (authHeader != null) {
                for (Cookie cookie : authHeader) {
                    log.info("str => {}", cookie.getName());
                    if ("authorization".equals(cookie.getName())) {
                        checkingRefToken = cookie.getValue();
                    }
                }
            }

            if (authHeader != null) {
                refTokenEntity.setRef_token(checkingRefToken);
                RefTokenEntity checkedRefToken = iUserMemberDaoMapper.selectRefToken(refTokenEntity);
                if (checkedRefToken != null) {
                    int result = iUserMemberDaoMapper.deleteDupRefToken(checkedRefToken);
                    if (result > 0) {
                        log.info("중복 refToken 삭제 완료");
                    } else {
                        log.info("중복 refToken 삭제 실패");
                    }
                }
            }

            List<GrantedAuthority> authorities = Arrays.asList(
                    new SimpleGrantedAuthority("ROLE_USER")
            );

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userMemberDto.getU_m_email(), null, authorities);


            String accessToken = jwtProvider.createAccessToken(userMemberDto.getU_m_email(), secretKey, authenticationToken.getAuthorities());
            String refreshToken = jwtProvider.createRefreshToken(userMemberDto.getU_m_email(), secretKey);

            refTokenEntity.setRef_token(refreshToken);
            // refresh token -> tbl_tokens에 저장
            int result = iUserMemberDaoMapper.insertRefToken(refTokenEntity);
            if (result <= 0) {
                log.info("Ref Token 등록 실패");
            } else {
                log.info("Ref Token 등록 성공");
            }

            map.put("accessToken", accessToken);
            map.put("refreshToken", refreshToken);

        } else {
            map.put("result", "incorrectIdOrPw");

        }

        return map;
    }

    @Override
    public int modify(Map<String, Object> msgMap, UserMemberDto userMemberDto, HttpServletRequest request) {

        log.info("modify");

        String refreshToken = null;
        Cookie[] authHeader = request.getCookies();
        if (authHeader != null) {
            for (Cookie cookie : authHeader) {
                log.info("str => {}", cookie.getName());
                if ("authorization".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        final String userEmail;
        // ref token에서 userEmail 추출
        userEmail = jwtAuthenticationFilter.getUserEmail(secretKey, refreshToken);

        userMemberDto.setU_m_email(userEmail);
        userMemberDto.setU_m_name(msgMap.get("name").toString());
        userMemberDto.setU_m_phone(msgMap.get("phone").toString());
        userMemberDto.setU_m_nickname(msgMap.get("nickname").toString());

        int result = iUserMemberDaoMapper.updateUser(userMemberDto);
        if (result == Constant.USER_MODIFY_FAIL) {
            return Constant.USER_MODIFY_FAIL;
        }

        return Constant.USER_MODIFY_SUCCESS;
    }

    @Override
    public UserMemberDto userInfo(HttpServletRequest request, UserMemberDto userMemberDto) {
        log.info("userInfo");
        String checkingRefToken = null;
        Cookie[] authHeader = request.getCookies();
        log.info("userInfo Service = {}", (Object) authHeader);
        if (authHeader != null) {
            for (Cookie cookie : authHeader) {
                log.info("str => {}", cookie.getName());
                if ("authorization".equals(cookie.getName())) {
                    checkingRefToken = cookie.getValue();
                }
            }
        }


        if (authHeader != null) {

            userMemberDto.setU_m_email(jwtAuthenticationFilter.getUserEmail(secretKey, checkingRefToken));

            UserMemberDto userInfo = iUserMemberDaoMapper.isMemberUser(userMemberDto);

            userInfo.setU_m_pw("******");

            return userInfo;

        }
        return null;

    }

    @Override
    public Map<String, Object> refreshToken(HttpServletRequest request, HttpServletResponse response, RefTokenEntity refTokenEntity) {
        log.info("refreshToken");

        Map<String, Object> map = new HashMap<>();
        String refreshToken = null;
        Cookie[] authHeader = request.getCookies();
        if (authHeader != null) {
            for (Cookie cookie : authHeader) {
                log.info("str => {}", cookie.getName());
                if ("authorization".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        final String userEmail;
        if (authHeader == null) {
            map.put("result", "RefTokenNullInCookie");
            return map;
        }


        // token에 동일한 refresh token 명이 있는지 check
        // 있으면 이후 작업 진행, -> DB 중복 ref token delete -> 새로 발급받은 ref token insert
        // 없으면 이미 로그아웃 또는 회원 탈퇴를 진행한 회원이라고 판단했기 때문에 오류 코드 발생.
        refTokenEntity.setRef_token(refreshToken);
        RefTokenEntity checkRefToken = iUserMemberDaoMapper.selectRefToken(refTokenEntity);
        if (checkRefToken == null) {
            map.put("result", "RefTokenNullInDB");
            return map;
        }

        // 중복 ref token delete
        refTokenEntity.setRef_token(checkRefToken.getRef_token());
        int result = iUserMemberDaoMapper.deleteDupRefToken(checkRefToken);
        if (result > 0) {
            log.info("중복 refToken 삭제 완료");
        } else {
            log.info("중복 refToken 삭제 실패");
        }

        userEmail = jwtAuthenticationFilter.getUserEmail(secretKey, refreshToken);
        if (userEmail != null) {
            if (jwtAuthenticationFilter.validate(secretKey, refreshToken)) {
                log.error("refreshToken이 만료되었습니다.");
                map.put("result", "RefTokenExpired");
                return map;

            } else {

                // 재발급 받은 Ref Token insert
                List<GrantedAuthority> authorities = Arrays.asList(
                        new SimpleGrantedAuthority("ROLE_USER")
                );

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userEmail, null, authorities);


                String ReAccessToken = jwtProvider.createAccessToken(userEmail, secretKey, authenticationToken.getAuthorities());
                String ReRefreshToken = jwtProvider.createRefreshToken(userEmail, secretKey);

                refTokenEntity.setRef_token(ReRefreshToken);
                // refresh token -> tbl_tokens에 저장
                result = iUserMemberDaoMapper.insertRefToken(refTokenEntity);
                if (result <= 0) {
                    log.info("Ref Token 등록 실패");
                } else {
                    log.info("Ref Token 등록 성공");
                }

                map.put("ReAccessToken", ReAccessToken);
                map.put("ReRefreshToken", ReRefreshToken);

            }
        }
        return map;
    }

    @Override
    public String changePw(Map<String, Object> msgMap, HttpServletRequest request, UserMemberDto userMemberDto) {
        log.info("changePw");

        userMemberDto.setU_m_pw(msgMap.get("beforePw").toString());

        String checkingRefToken = null;
        Cookie[] authHeader = request.getCookies();
        if (authHeader != null) {
            for (Cookie cookie : authHeader) {
                log.info("str => {}", cookie.getName());
                if ("authorization".equals(cookie.getName())) {
                    checkingRefToken = cookie.getValue();
                }
            }
        }

            userMemberDto.setU_m_email(jwtAuthenticationFilter.getUserEmail(secretKey, checkingRefToken));

            UserMemberDto idVerifiedUserMemberDto = iUserMemberDaoMapper.isMemberUser(userMemberDto);
            if (idVerifiedUserMemberDto != null && passwordEncoder.matches(userMemberDto.getU_m_pw(), idVerifiedUserMemberDto.getU_m_pw())) {
                userMemberDto.setU_m_pw(passwordEncoder.encode(msgMap.get("afterPw").toString()));
                int result = iUserMemberDaoMapper.updateUserForPw(userMemberDto);
                if (result > 0) {
                    return "UserChangePwSuccess";
                } else {
                    return "UserChangePwFail";
                }
            }


        return null;
    }

    @Override
    public String logout(HttpServletRequest request, HttpServletResponse response, RefTokenEntity refTokenEntity) {
        log.info("logout");

        String checkingRefToken = null;
        Cookie[] authHeader = request.getCookies();
        if (authHeader != null) {
            for (Cookie cookie : authHeader) {
                log.info("str ==> {}", cookie.getName());
                if ("authorization".equals(cookie.getName())) {
                    log.info("tp1");
                    checkingRefToken = cookie.getValue();
                }
            }
        }
        log.info("tp2");
        if (authHeader != null) {
            log.info("tp3");

            refTokenEntity.setRef_token(checkingRefToken);
            RefTokenEntity checkedRefToken = iUserMemberDaoMapper.selectRefToken(refTokenEntity);
            log.info("tp4");
            if (checkedRefToken != null) {
                int result = iUserMemberDaoMapper.deleteDupRefToken(checkedRefToken);
                if (result > 0) {
                    log.info("중복 refToken 삭제 완료");
                    return "중복 refToken 삭제 완료";
                } else {
                    log.info("중복 refToken 삭제 실패");
                    return "중복 refToken 삭제 실패";
                }
            }
        }
        return "token 값이 잘못되었습니다.";
    }

    @Override
    public String signOut(HttpServletRequest request, HttpServletResponse response, UserMemberDto userMemberDto, RefTokenEntity refTokenEntity) {
        log.info("signOut");

        String refreshToken = null;
        Cookie[] authHeader = request.getCookies();
        if (authHeader != null) {
            for (Cookie cookie : authHeader) {
                log.info("str => {}", cookie.getName());
                if ("authorization".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        final String userEmail;
        if (authHeader == null) {
            log.info("authHeaderNull");
            return "refresh token is null";
        }

        // ref token에서 userEmail 추출
        userEmail = jwtAuthenticationFilter.getUserEmail(secretKey, refreshToken);

        // userEmail 이 동일한 사람 DB에서 삭제
        log.info("userEmail = {}", userEmail);
        userMemberDto.setU_m_email(userEmail);
        int deleteMemberResult = iUserMemberDaoMapper.deleteMember(userMemberDto);
        if (deleteMemberResult <= 0) {
            log.info("delete Member fail");
            return "회원탈퇴 실패";
        }

        refTokenEntity.setRef_token(refreshToken);
        log.info("refreshToken = {}", refreshToken);
        RefTokenEntity checkedRefToken = iUserMemberDaoMapper.selectRefToken(refTokenEntity);
        if (checkedRefToken != null) {
            int result = iUserMemberDaoMapper.deleteDupRefToken(checkedRefToken);
            if (result > 0) {
                log.info("중복 refToken 삭제 완료");
            } else {
                log.info("중복 refToken 삭제 실패");
                return "중복 refToken 삭제 실패";
            }
        }

        return "회원 탈퇴 성공";
    }


}
