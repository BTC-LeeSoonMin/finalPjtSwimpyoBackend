package com.btc.swimpyo.backend.service.mypage.user;

import com.btc.swimpyo.backend.config.aspect.SlackNotification;
import com.btc.swimpyo.backend.dto.reservation.ReservationDto;
import com.btc.swimpyo.backend.dto.room.admin.AdminRoomDto;
import com.btc.swimpyo.backend.dto.room.user.UserReviewDto;
import com.btc.swimpyo.backend.mappers.mypage.user.IUserMypageDaoMapper;
import com.btc.swimpyo.backend.mappers.review.IUserReviewDaoMapper;
import com.btc.swimpyo.backend.utils.jwt.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserMypageService implements IUserMypageService {

    @Value("${secret-key}")
    private String secretKey;

    private final IUserMypageDaoMapper iUserMypageDaoMapper;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final IUserReviewDaoMapper iUserReviewDaoMapper;


    @Override
    @SlackNotification
    public List<Map<String,Object>> GetRezList(HttpServletRequest request, int u_r_no, int period) {
        log.info("GetRezList");
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
//        final String userEmail;
//        userEmail = jwtAuthenticationFilter.getUserEmail(secretKey, refreshToken);

//        map.put("userEmail", userEmail);
        map.put("u_r_no", u_r_no);
        map.put("period", period);

        return iUserMypageDaoMapper.selectRezList(map);
    }

//@Override
//@SlackNotification
//public List<Map<String,Object>> GetRezList(HttpServletRequest request, int u_r_no, int period) {
//    log.info("GetRezList");
//    Map<String, Object> map = new HashMap<>();
//    List<Map<String,Object>> list = new ArrayList<>();
//
//    String refreshToken = null;
//    Cookie[] authHeader = request.getCookies();
//    if (authHeader != null) {
//        for (Cookie cookie : authHeader) {
//            log.info("str => {}", cookie.getName());
//            if ("authorization".equals(cookie.getName())) {
//                refreshToken = cookie.getValue();
//            }
//        }
//    }
//    map.put("u_r_no", u_r_no);
//    map.put("period", period);
//
//    list.addAll(iUserMypageDaoMapper.selectRezListURTbl(map));
//    list.addAll(iUserMypageDaoMapper.selectRezListARtbl(map));
//    list.addAll(iUserMypageDaoMapper.selectRezListAATbl(map));
//    list.addAll(iUserMypageDaoMapper.selectRezListAITbl(map));
//
//    return list;
//}


    @Override
    public Map<String,Object> showReviewList(HttpServletRequest request) {
        log.info("showReviewList");

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
        userEmail = jwtAuthenticationFilter.getUserEmail(secretKey, refreshToken);

        Map<String, Object> msgData = new HashMap<>();
        List<Map<String, Object>> ResultUserReviewImgList = new ArrayList<>();

        List<Map<String, Object>> userReviewList = iUserMypageDaoMapper.selectReviewInfo(userEmail);
        ArrayList<Integer> rNoList = new ArrayList<>();

        for (Map<String, Object> review : userReviewList) {
            if (review.containsKey("r_no")) {
                rNoList.add((Integer) review.get("r_no"));
            }
        }
        log.info("rNoList: = {} ", rNoList);

        int r_no;
        for(int i = 0; i< rNoList.size(); i++){
            r_no = rNoList.get(i);
            log.info("r_no: " + r_no);

            ResultUserReviewImgList.addAll(iUserMypageDaoMapper.selectReviewImgForList(r_no));


        }
        log.info("ResultUserReviewImgList: " + ResultUserReviewImgList);

        msgData.put("userReviewList", userReviewList);
        msgData.put("userReviewImgList", ResultUserReviewImgList);

        return msgData;

    }

    @Override
    public List<Map<String, Object>> GetRezDetail(HttpServletRequest request, int u_r_no) {
        log.info("GetRezDetail");

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
        userEmail = jwtAuthenticationFilter.getUserEmail(secretKey, refreshToken);


        map.put("userEmail", userEmail);
        map.put("u_r_no", u_r_no);

        List<Map<String, Object>> maplist = iUserMypageDaoMapper.selectRezDetail(map);
//        map = iUserMypageDaoMapper.selectRezDetail(map);
        log.info("maplist = {}", maplist);

//        // 날짜 파싱 및 일수 계산
//        LocalDate startDay = LocalDate.parse(map.get("u_r_check_in").toString());
//        LocalDate endDay = LocalDate.parse(map.get("u_r_check_out").toString());
//        int days = (int) ChronoUnit.DAYS.between(startDay, endDay);
//
//        log.info("days = {}", days);
//        if(days > 1){
//            map.put("a_r_pride", Integer.parseInt(map.get("a_r_pride").toString()) * days);
//        }

        return maplist;

    }
}
