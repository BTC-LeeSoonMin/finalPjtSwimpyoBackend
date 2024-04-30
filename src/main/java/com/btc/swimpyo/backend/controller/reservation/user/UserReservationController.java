package com.btc.swimpyo.backend.controller.reservation.user;

import com.btc.swimpyo.backend.dto.kakaoPay.AmountDto;
import com.btc.swimpyo.backend.dto.kakaoPay.KakaoApproveResponseDto;
import com.btc.swimpyo.backend.dto.reservation.ReservationDto;
import com.btc.swimpyo.backend.service.Reservation.user.UserReservationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@Log4j2
@RequestMapping("/api/user/reservation")
@RequiredArgsConstructor
public class UserReservationController {

    @Value("${server.serverAddress}")
    private String serverAddress;

    private final UserReservationService userReservationService;

    // 기간(날짜), 방 체크(예약이 되어 있는지)
    // 1. 예약은 룸 상세 페이지에서 가능하다.
    // 2. 예약 가능한 방만 예약할 수 있다.
    // 3. 예약이 가능하다면,날짜, 숙박/대실, 가격을 선택한다.
    // 4. 선택 후 결제하기 버튼을 클릭하면 결제 페이지로 이동한다.
    // 5. 결제 페이지에는 룸 정보, 예약 날짜, 가격이 나오고, 실사용자의 이름과 연락처를 입력한다.
    // 6. 모두 입력하면 결제하기 버튼을 통해 카카오페이로 결제할 수 있다.

    /*
     * 예약하기 버튼 클릭 시 예약 페이지(모달창) -> 모달창에서 넘어온 값을 가져와서!!!
     * 이게 예약 페이지에 쓰일 데이터를 뿌려주는 곳!!
     * 방 상세 정보 + 모달창에서 받은 정보가 담겨 있어야 함 .
     */
    @PostMapping("/ready")
    @Transactional
    public Map<String, Object> createRsvReady(@RequestPart ReservationDto reservationDto){
        log.info("[UserReservationController] createRsvReady()");
        log.info("[UserReservationController] adminReservaitionDto(): " + reservationDto);

        return userReservationService.createRsvReady(reservationDto);

    }

    // 결제
    @PostMapping("")
    public Map<String, Object> createRsvApproval(@RequestPart ReservationDto reservationDto) {
        log.info("[UserReservationController] createRsvApproval()");
        log.info("[UserReservationController] adminReservaitionDto(): " + reservationDto);
        log.info("[UserReservationController] u_m_email(): " + reservationDto.getU_m_email());
        log.info("[UserReservationController] a_r_name(): " + reservationDto.getA_r_name());

        return userReservationService.createRsvApproval(reservationDto);

    }

    // 결제 승인
    @GetMapping("/registConfirm")
    public Map<String, Object> registRsv(@RequestParam ("pg_token") String pg_token, @RequestParam("partner_order_id") String partner_order_id, HttpServletResponse response ) throws IOException {
        log.info("[UserReservationController] registRsv()");
        
//        response.sendRedirect("http://localhost:3000/payment/success");
//        response.sendRedirect("http://" + ("localhost".equals(serverAddress) ? "localhost:3000" : serverAddress + "/payment/success"));
        response.sendRedirect("https://soonmin.info/payment/success");


        return userReservationService.registRsv(pg_token, partner_order_id);

    }

    // 결제 승인 후 front에 success 메세지
//    @GetMapping("/success")
//    public String success() {
//        log.info("[UserReservationController] success()");
//
//        return userReservationService.success();
//
//    }

    /*
     * front에서 받아야 하는 값 : 예약번호(u_r_no), tid, u_m_email 값..?
     */
    // 결제 취소
    @PostMapping("refund")
    public void refundRsv(@RequestPart KakaoApproveResponseDto kakaoApproveResponseDto, AmountDto amountDto, @RequestParam(value = "deleteRsvNo", required = false) int deleteRsvNo){
        log.info("[UserReservationController] refundRsv()");
        log.info("[UserReservationController] kakaoApproveResponseDto: " + kakaoApproveResponseDto);
        log.info("[UserReservationController] deleteRsvNo: " + deleteRsvNo);

        userReservationService.refundRsv(kakaoApproveResponseDto, amountDto, deleteRsvNo);

    }

}