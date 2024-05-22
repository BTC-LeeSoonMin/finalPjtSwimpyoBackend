package com.btc.swimpyo.backend.mappers.accm.user;

import com.btc.swimpyo.backend.dto.accm.admin.AdminAccmDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ISearchAccmDaoMapper {
    //    List<AdminAccmDto> selectAccms(Map<String, Object> msgMap);
    List<Map<String, Object>> selectAccms(Map<String, Object> msgMap);

    List<AdminAccmDto> mapInfoList(String region);

    List<Map<String, Object>> selectTop3Accm(Map<String, Object> params);

    List<Map<String, Object>> top3AccommodationList();

    List<Map<String, Object>> getaAccNo(List<Integer> aRNoList);

    List<Map<String, Object>> getHotelName(Map<String, Object> params);

    List<Map<String, Object>> getImage(List<Integer> getImageAccNoList);

    List<Map<String, Object>> getStaticResult();

    List<Map<String, Object>> getDynamicResult(String accmValue);
}
