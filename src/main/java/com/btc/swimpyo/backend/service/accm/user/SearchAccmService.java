package com.btc.swimpyo.backend.service.accm.user;

import com.btc.swimpyo.backend.dto.accm.admin.AdminAccmDto;
import com.btc.swimpyo.backend.mappers.accm.user.ISearchAccmDaoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class SearchAccmService implements ISearchAccmService {

    private final ISearchAccmDaoMapper iSearchAccmDaoMapper;

    @Override
    public List<Map<String, Object>> searchAccm(Map<String, Object> msgMap, AdminAccmDto adminAccmDto) {
        log.info("searchAccm - tp =-----> {}", msgMap);

        // 날짜 파싱 및 일수 계산
        LocalDate startDay = LocalDate.parse(msgMap.get("startDay").toString());
        LocalDate endDay = LocalDate.parse(msgMap.get("endDay").toString());
        int days = (int) ChronoUnit.DAYS.between(startDay, endDay);

        // priceOrder가 null이 아닐 경우 파싱
        if (msgMap.get("priceOrder") != null) {
            msgMap.put("priceOrder", Integer.parseInt(msgMap.get("priceOrder").toString()));
        }

        log.info("msgMap + Days => {}", msgMap);

        // 숙박 시설 데이터 검색
        List<Map<String, Object>> selectAccms = iSearchAccmDaoMapper.selectAccms(msgMap);
        log.info("selectAccms => {}", selectAccms);

        if (selectAccms == null) {
            return List.of(); // 검색 결과가 없을 경우 빈 리스트 반환
        }

        // Days가 2 이상일 때
        if (selectAccms != null && (days > 1)) {
            // 동일한 이름을 가진 숙박 시설의 가격을 합산
            List<Map<String, Object>> sortedList = new ArrayList<>(selectAccms.stream()
                    .collect(Collectors.toMap(
                            accm -> (String) accm.get("a_acc_name"),
                            Function.identity(),
                            (accm1, accm2) -> {
                                accm1.put("a_r_price", Integer.parseInt(accm1.get("a_r_price").toString())
                                        + Integer.parseInt(accm2.get("a_r_price").toString()) * days);
                                return accm1;})).values());
            if (Integer.parseInt(msgMap.get("priceOrder").toString()) == 0) {
                sortedList.sort((accm1, accm2) -> ((Integer) accm2.get("a_r_price")).compareTo((Integer) accm1.get("a_r_price")));
            } else if (Integer.parseInt(msgMap.get("priceOrder").toString()) == 1) {
                sortedList.sort(Comparator.comparingInt(accm -> (Integer) accm.get("a_r_price")));
            }

            return sortedList;

        }

        // Days가 1 일 때
        List<Map<String, Object>> sortedList = new ArrayList<>(selectAccms.stream()
                .collect(Collectors.toMap(
                        accm -> (String) accm.get("a_acc_name"),
                        Function.identity(),
                        (accm1, accm2) -> accm1
                ))
                .values());

        if (Integer.parseInt(msgMap.get("priceOrder").toString()) == 1) {
            sortedList.sort((accm1, accm2) -> ((Integer) accm2.get("a_r_price")).compareTo((Integer) accm1.get("a_r_price")));
        } else if (Integer.parseInt(msgMap.get("priceOrder").toString()) == 0) {
            sortedList.sort(Comparator.comparingInt(accm -> (Integer) accm.get("a_r_price")));
        }

        return sortedList;

    }

    @Override
    public List<AdminAccmDto> mapInfoList(String region) {
        log.info("mapInfoList");

        return iSearchAccmDaoMapper.mapInfoList(region);
    }


    @Override
    public List<Map<String, Object>> usersStatic() {
        log.info("usersStatic");

        // 정적 쿼리 실행
        long startTimeStatic = System.currentTimeMillis();
        List<Map<String, Object>> usersStatic = iSearchAccmDaoMapper.getStaticResult();
        long endTimeStatic = System.currentTimeMillis();
        System.out.println("Static Query Time: " + (endTimeStatic - startTimeStatic) + "ms");
        return null;
    }

    @Override
    public Object usersDynamic(String accmValue) {
        log.info("usersDynamic");

        // 동적 쿼리 실행
        long startTimeDynamic = System.currentTimeMillis();
        List<Map<String, Object>> usersDynamic = iSearchAccmDaoMapper.getDynamicResult(accmValue);
        long endTimeDynamic = System.currentTimeMillis();
        System.out.println("Dynamic Query Time: " + (endTimeDynamic - startTimeDynamic) + "ms");

        return null;
    }

    @Override
    public List<Map<String, Object>> rankAccmList(String accmValue) {
        log.info("rankAccmList");
        Map<String, Object> params = new HashMap<>();

        // 예약수가 높은 객실순
        List<Map<String, Object>> top3AccommodationList = iSearchAccmDaoMapper.top3AccommodationList();

        // 결과를 가공하여 a_r_no만 추출하여 리스트로 반환
        List<Integer> aRNoList = new ArrayList<>();
        List<Long> duplicationCountList = new ArrayList<>();
        for (Map<String, Object> accommodation : top3AccommodationList) {
            Integer aRNo = (Integer) accommodation.get("a_r_no");
            aRNoList.add(aRNo);
        }
        for (Map<String, Object> accommodation : top3AccommodationList) {
            Long count = (Long) accommodation.get("duplication_count");
            duplicationCountList.add(count);
        }

        log.info("aRnoList = {}", aRNoList);
        log.info("duplicationCountList = {}", duplicationCountList);


        params.put("aRNoList", aRNoList);
        params.put("accmValue", accmValue);

        log.info("params = {}", params);

        List<Map<String, Object>> accmList = iSearchAccmDaoMapper.selectTop3Accm(params);
        log.info("accmList = {}", accmList);

//        log.info("tp = {}", top3AccommodationList.get(0).get("a_acc_no");

        // 결과를 가공하여 리스트로 반환
        List<Map<String, Object>> processedAccommodationList = new ArrayList<>();
        for (int i = 0; i < top3AccommodationList.size(); i++) {
            Map<String, Object> processedAccommodation = new HashMap<>();
            processedAccommodation.put("예약수", duplicationCountList.get(i));
            processedAccommodation.put("a_acc_no", accmList.get(i).get("a_acc_no"));
            processedAccommodation.put("a_i_image", accmList.get(i).get("a_i_image"));
            processedAccommodation.put("a_acc_name", accmList.get(i).get("a_acc_name"));
            processedAccommodationList.add(processedAccommodation);
        }
        log.info("processedAccommodationList = {}", processedAccommodationList);
        return processedAccommodationList;
    }

//    @Override
//    public List<Map<String, Object>> rankAccmList(String accmValue) {
//        log.info("rankAccmList");
//        Map<String, Object> params = new HashMap<>();
//
//        // 예약수가 높은 객실순
//        List<Map<String, Object>> top3AccommodationList = iSearchAccmDaoMapper.top3AccommodationList();
//
//        // 결과를 가공하여 a_r_no만 추출하여 리스트로 반환
//        List<Integer> aRNoList = new ArrayList<>();
//        List<Long> duplicationCountList = new ArrayList<>();
//        for (Map<String, Object> accommodation : top3AccommodationList) {
//            Integer aRNo = (Integer) accommodation.get("a_r_no");
//            aRNoList.add(aRNo);
//        }
//        for (Map<String, Object> accommodation : top3AccommodationList) {
//            Long count = (Long) accommodation.get("duplication_count");
//            duplicationCountList.add(count);
//        }
//        log.info("aRnoList = {}", aRNoList);
//        log.info("duplicationCountList = {}", duplicationCountList);
//
//        // 예약수가 높은 객실순으로 a_acc_no 가져오기
//        List<Map<String, Object>> getaAccNo = iSearchAccmDaoMapper.getaAccNo(aRNoList);
//
//        List<Integer> aAccNoList = new ArrayList<>();
//        for (Map<String, Object> item : getaAccNo) {
//            Integer aAccNo = (Integer) item.get("a_acc_no");
//            aAccNoList.add(aAccNo);
//        }
//        log.info("aAccNoList = {}", aAccNoList);
//        log.info("accmValue = {}", accmValue);
//        params.put("aAccNoList", aAccNoList);
//        params.put("accmValue", accmValue);
//
//        // -- a_acc_no에 해당하는 a_acc_no와 호텔명 가져오기
//        List<Map<String, Object>> getHotelName = iSearchAccmDaoMapper.getHotelName(params);
//
//        List<String> hotelNameList = new ArrayList<>();
//        for (Map<String, Object> name : getHotelName) {
//            String nameList = (String) name.get("a_acc_name");
//            hotelNameList.add(nameList);
//        }
//
//        List<Integer> getImageAccNoList = new ArrayList<>();
//        for (Map<String, Object> hotel : getHotelName) {
//            Integer aAccNo = (Integer) hotel.get("a_acc_no");
//            getImageAccNoList.add(aAccNo);
//        }
//        log.info("getImageAccNoList = {}", getImageAccNoList);
//        log.info("hotelNameList = {}", hotelNameList);
//
//        // a_acc_no에 해당하는 a_i_image 가져오기
//        List<Map<String, Object>> getImage = iSearchAccmDaoMapper.getImage(getImageAccNoList);
//        List<String> imageList = new ArrayList<>();
//        for (Map<String, Object> image : getImage) {
//            String address = (String) image.get("a_i_image");
//            imageList.add(address);
//        }
//        log.info("imageList = {}", imageList);
//
//        // 결과를 가공하여 리스트로 반환
//        List<Map<String, Object>> processedAccommodationList = new ArrayList<>();
//        for (int i = 0; i < top3AccommodationList.size(); i++) {
//            Map<String, Object> processedAccommodation = new HashMap<>();
//            processedAccommodation.put("예약수", duplicationCountList.get(i));
//            processedAccommodation.put("a_acc_no", aAccNoList.get(i));
//            processedAccommodation.put("a_i_image", imageList.get(i));
//            processedAccommodation.put("a_acc_name", hotelNameList.get(i));
//            processedAccommodationList.add(processedAccommodation);
//        }
//        log.info("processedAccommodationList = {}", processedAccommodationList);
//
//        return processedAccommodationList;
//    }


}
