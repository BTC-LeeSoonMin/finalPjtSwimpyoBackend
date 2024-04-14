package com.btc.swimpyo.backend.controller.mypage.user;

import com.btc.swimpyo.backend.config.aspect.SlackNotification;
import com.btc.swimpyo.backend.dto.reservation.ReservationDto;
import com.btc.swimpyo.backend.dto.room.user.UserReviewDto;
import com.btc.swimpyo.backend.service.mypage.user.UserMypageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/user/mypage")
@RequiredArgsConstructor
public class UserMypageController {

    private final UserMypageService userMypageService;

    // 사용자 마이페이지 예약 리스트
    @GetMapping("/GetRezList")
    public Object GetRezList(@RequestParam(value = "u_r_no", defaultValue="0") int u_r_no,
                             @RequestParam(value = "period", defaultValue = "0") int period,
                             HttpServletRequest request){
        log.info("GetRezList");

        List<Map<String,Object>> GetRezList = userMypageService.GetRezList(request, u_r_no, period);
        if(GetRezList == null){
            return "GetRezListFail";
        }
        return GetRezList;
    }

    // 사용자 마이페이지 예약 상세보기
    @GetMapping("/GetRezDetail")
    public Object GetRezDetail(@RequestParam(value = "u_r_no", defaultValue="0") int u_r_no,
                               HttpServletRequest request){
        log.info("GetRezDetail");

        log.info("GetRezDetail uRNo = {}", u_r_no);

        List<Map<String, Object>> GetRezDetail = userMypageService.GetRezDetail(request, u_r_no);
        if(GetRezDetail == null){
            return "GetRezDetailFail";
        }
        return GetRezDetail;
    }


    // 사용자 마이페이지 리뷰 리스트
    @PostMapping("/getReviewList")
    public Object showReviewList(HttpServletRequest request) {
        log.info("showReviewList");

        Map<String,Object> map = userMypageService.showReviewList(request);
        if(map == null){
            return "getReviewListFail";
        }
        return map;

    }

}
