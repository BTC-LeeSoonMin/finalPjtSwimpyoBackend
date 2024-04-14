package com.btc.swimpyo.backend.mappers.mypage.user;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface IUserMypageDaoMapper {
    List<Map<String,Object>> selectRezList(Map<String, Object> map);
//    List<Map<String,Object>> selectRezListURTbl(Map<String, Object> map);
//    List<Map<String,Object>> selectRezListARtbl(Map<String, Object> map);
//    List<Map<String,Object>> selectRezListAATbl(Map<String, Object> map);
//    List<Map<String,Object>> selectRezListAITbl(Map<String, Object> map);

    List<Map<String, Object>> selectRezDetail(Map<String, Object> map);

    List<Map<String, Object>> selectReviewInfo(String userEmail);

    List<Integer> selectReviewImgNo(int rNo);

    List<Map<String, Object>> selectReviewImgForList(int rNo);
}
