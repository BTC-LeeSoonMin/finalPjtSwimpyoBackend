package com.btc.swimpyo.backend.controller.member.user;


import com.btc.swimpyo.backend.config.Constant;
import com.btc.swimpyo.backend.dto.member.admin.AdminMemberDto;
import com.btc.swimpyo.backend.dto.member.user.UserMemberDto;
import com.btc.swimpyo.backend.service.member.admin.IAdminMemberService;
import com.btc.swimpyo.backend.service.member.user.IUserMemberService;
import com.btc.swimpyo.backend.utils.jwt.entity.RefTokenEntity;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/user/member")
@RequiredArgsConstructor
public class UserMemberController {

    private final IUserMemberService iUserMemberService;

    // 사용자 회원가입
    @PostMapping("/signUp")
    public String signUp(@RequestBody Map<String, Object> msgMap, UserMemberDto userMemberDto) {
        log.info("signUp");
        int response = iUserMemberService.signUp(msgMap, userMemberDto);
        log.info("response : {}", response);

        if(response == Constant.ADMIN_SIGNUP_SUCCESS){
            return "MemberUserSignUpSuccess";
        } else if(response == Constant.ADMIN_DUP_MEMBER){
            return "MemberUserDup";
        } else {
            return "MemberUserSignUpFail";
        }
    }

    // 사용자 로그인
    @PostMapping("/signIn")
    public Object signIn(@RequestBody Map<String, Object> msgMap, UserMemberDto userMemberDto, RefTokenEntity refTokenEntity, HttpServletRequest request, HttpServletResponse response) {
        log.info("signIn");
        log.info("msgMap >>>>>>> : {}", msgMap);

        Map<String, Object> map = iUserMemberService.signIn(msgMap, userMemberDto, refTokenEntity, request,response);
        if (map != null) {
            if (map.get("result") == "incorrectIdOrPw") {
                return "IncorrectIdOrPw";
            }
            if (map.get("result") == "MemberUserNull") {
                return "MemberUserLoginNull";
            }

            // 로그인 성공했기때문에 accessToken, refreshToken 발급
            String refreshTokenValue = map.get("refreshToken").toString();
            log.info("refreshTokenValue");
            Cookie cookie = new Cookie("authorization", refreshTokenValue);
            cookie.setHttpOnly(true);  //httponly 옵션 설정
            cookie.setSecure(true); //https 옵션 설정
            cookie.setMaxAge(24 * 3600);
            cookie.setPath("/"); // 모든 곳에서 쿠키열람이 가능하도록 설정
            response.addCookie(cookie);

            return map.get("accessToken");

        } else {
            return "MemberUserLoginFail";
        }

    }

    // 사용자 회원수정
    @PostMapping("/modify")
    public Object modify(@RequestBody Map<String, Object> msgMap, UserMemberDto userMemberDto, HttpServletRequest request){
        log.info("modify");

        int result;
        result = iUserMemberService.modify(msgMap, userMemberDto, request);
        if(result == 0){
            return "MemberUserModifyFail";
        } else if (result == 1) {
            return "MemberUserModifySuccess";
        }

        return result;
    }

    // 사용자 정보 요청
    @PostMapping("/userInfo")
    public Object userInfo(HttpServletRequest request,UserMemberDto userMemberDto){
        log.info("userInfo");

        UserMemberDto userInfo = iUserMemberService.userInfo(request, userMemberDto);
        if(userInfo == null){
            return null;
        }
        return userInfo;
    }

    // 사용자 리프레쉬 토큰 요청
    @PostMapping("/refreshToken")
    public Object refreshToken(HttpServletRequest request, HttpServletResponse response, RefTokenEntity refTokenEntity) {
        log.info("refreshToken in");

        Map<String, Object> map = iUserMemberService.refreshToken(request, response, refTokenEntity);
        if(map.get("result") == "RefTokenNull"){

            return "RefTokenNullInCookie";
        }
        if(map.get("result") == "RefTokenNullInDB"){

            return "RefTokenNullInDB";
        }
        if(map.get("result") == "RefTokenExpired"){

            return "RefTokenExpired";
        }

        String refreshTokenValue = (String) map.get("ReRefreshToken");
        log.info("ReRefreshToken");
        Cookie cookie = new Cookie("authorization", refreshTokenValue);
        cookie.setHttpOnly(true);  //httponly 옵션 설정
        cookie.setSecure(true); //https 옵션 설정
        cookie.setMaxAge(24 * 3600);
        cookie.setPath("/"); // 모든 곳에서 쿠키열람이 가능하도록 설정
        response.addCookie(cookie);

        return map.get("ReAccessToken");

    }

    // 사용자 회원 탈퇴
    @PostMapping("/signout")
    public Object signOut(HttpServletRequest request,
                          HttpServletResponse response,
                          UserMemberDto userMemberDto,
                          RefTokenEntity refTokenEntity) {
        log.info("signOut");

        String result = iUserMemberService.signOut(request, response, userMemberDto, refTokenEntity);
        if(result == "중복 refToken 삭제 실패"){
            log.info("중복 refToken 삭제 실패");
            return "signOutFail";
        }
        if(result == "refresh token is null"){
            log.info("refresh token is null");
            return "RefTokenNullInCookie";
        }

        // Cookie 삭제
        Cookie myCookie = new Cookie("authorization", null);
        myCookie.setMaxAge(0); // 쿠키의 expiration 타임을 0으로 하여 없앤다.
        myCookie.setPath("/"); // 모든 경로에서 삭제 됬음을 알린다.
        response.addCookie(myCookie);

        return "signOutSuccess";
    }


    // 사용자 비밀번호 변경
    @PostMapping("/changePw")
    public Object changePw(@RequestBody Map<String, Object> msgMap,HttpServletRequest request,UserMemberDto userMemberDto){
        log.info("changePw");

        String result = iUserMemberService.changePw(msgMap, request, userMemberDto);

        if(result == "UserChangePwSuccess") {
            return "UserChangePwSuccess";
        }
        if(result == "UserChangePwFail"){
            return "UserChangePwFail";
        }

        return "UserChangePwFail";
    }

    @PostMapping("/logout")
    public Object logout(HttpServletRequest request, HttpServletResponse response, RefTokenEntity refTokenEntity) {
        log.info("logout");

        String result = iUserMemberService.logout(request, response, refTokenEntity);
        if(result == "중복 refToken 삭제 실패"){
            log.info("중복 refToken 삭제 실패");
            return "logout fail";
        }
        if(result == "token 값이 잘못 되었습니다."){
            log.info("token 값이 잘못 되었습니다.");
            return "token 값이 잘못 되었습니다.";
        }

        // Cookie 삭제
        Cookie myCookie = new Cookie("authorization", null);
        myCookie.setMaxAge(0); // 쿠키의 expiration 타임을 0으로 하여 없앤다.
        myCookie.setPath("/"); // 모든 경로에서 삭제 됬음을 알린다.
        response.addCookie(myCookie);

        return "logout success";
    }

}
