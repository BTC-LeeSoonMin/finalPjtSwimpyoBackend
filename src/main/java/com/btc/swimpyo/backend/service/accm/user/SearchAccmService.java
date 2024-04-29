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
    public List<Map<String, Object>> rankAccmList(String accmValue) {
        log.info("rankAccmList");

        return iSearchAccmDaoMapper.selectTop3Accm(accmValue);
    }




}
