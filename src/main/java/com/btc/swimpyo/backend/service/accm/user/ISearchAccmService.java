package com.btc.swimpyo.backend.service.accm.user;

import com.btc.swimpyo.backend.dto.accm.admin.AdminAccmDto;

import java.util.List;
import java.util.Map;

public interface ISearchAccmService {
//    List<AdminAccmDto> searchAccm(Map<String, Object> msgMap, AdminAccmDto adminAccmDto);
List<Map<String, Object>> searchAccm(Map<String, Object> msgMap, AdminAccmDto adminAccmDto);

    List<AdminAccmDto> mapInfoList(String region);

    List<Map<String, Object>> rankAccmList(String accmValue);

    List<Map<String, Object>> usersStatic();

    Object usersDynamic(String accmValue);
}
