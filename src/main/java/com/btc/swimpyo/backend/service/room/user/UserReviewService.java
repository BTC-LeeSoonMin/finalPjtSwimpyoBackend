package com.btc.swimpyo.backend.service.room.user;

import com.btc.swimpyo.backend.controller.api.KakaoMapApiController;
import com.btc.swimpyo.backend.dto.room.user.UserReviewDto;
import com.btc.swimpyo.backend.mappers.review.IUserReviewDaoMapper;
import com.btc.swimpyo.backend.utils.S3Uploader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserReviewService implements IUserReviewService{

    private final IUserReviewDaoMapper iUserReviewDaoMapper;
    private final S3Uploader s3Uploader;
    private final KakaoMapApiController kakaoMapApiController;

    /*
     * 리뷰 등록
     * 1. 예약한 내역이 있는 사용자만 예약할 수 있다. -> 예약 번호 일치해야한다.
     * 2. 예약한 사용자의 email과 r_no가 일치해야만 등록할 수 있다.
     */
    // 등록
    @Override
    public String registConfirm(UserReviewDto userReviewDto, List<UserReviewDto> address, MultipartFile[] reviewImages) {
        log.info("[UserReviewService] registConfirm()");
        log.info("[registReview] userReviewDto : " + userReviewDto);
        log.info("[registReview] reviewImages : " + reviewImages);
        log.info("[registReview] r_xy_address : " + address);
        log.info("[registReview] reviewImages : " + reviewImages);

        String u_m_email = userReviewDto.getU_m_email();

        // a_r_no 가져오기
        iUserReviewDaoMapper.selectArNo(u_m_email);

        // 1. tbl_review 테이블에 데이터 등록
        int result = iUserReviewDaoMapper.insertReview(userReviewDto);
        log.info("result: " + result);

        // 2. 등록된 리뷰 테이블 번호 가져오기
        int r_no = iUserReviewDaoMapper.selectReviewNo(userReviewDto);
        log.info("r_no: " + r_no);
        userReviewDto.setR_no(r_no);
        log.info("r_no: " + userReviewDto.getR_no());

        if (address != null) {
            // 3. front에서 입력받은 주소 값 db에 저장
            for (int i = 0; i < address.size(); i++) {
                userReviewDto.setR_xy_address(address.get(i).getR_xy_address());
                userReviewDto.setR_xy_comment(address.get(i).getR_xy_comment());

                log.info("address:" + userReviewDto.getR_xy_address());
                log.info("comment:" + userReviewDto.getR_xy_comment());

                int isInsertAddress = iUserReviewDaoMapper.insertReviewAddress(userReviewDto);
                log.info("isInsertAddress:" + isInsertAddress);
                iUserReviewDaoMapper.updateIsWrite(r_no);

            }
        }

        // 4. tbl_review_image 테이블에 이미지 정보 등록
        if (reviewImages != null) {
            for (MultipartFile file : reviewImages) {
                log.info("reviewImages: " + reviewImages);

                String imageUrl = s3Uploader.uploadFileToS3(file, "static/test");

                userReviewDto.setR_ri_image(imageUrl);

                log.info("imageUrl: " + userReviewDto.getR_ri_image());

                int isInsertImg = iUserReviewDaoMapper.insertReviewImg(userReviewDto);
                log.info("isInsertImg:" + isInsertImg);
                iUserReviewDaoMapper.updateIsWrite(r_no);

            }
        } else {
            log.info(" \"reviewImages\" is null");

            return "success";

        }

        if(result > 0) {
            iUserReviewDaoMapper.updateIsWrite(r_no);
            return "success";

        } else {
            return "fail";

        }

    }

    // [숙박시설 상세페이지] 리스트 조회
    @Override
    public Map<String, Object> showReviewList(int a_acc_no) {
        log.info("[userReviewService] showReviewList()");

        Map<String, Object> msgData = new HashMap<>();
        List<UserReviewDto> ResultUserReviewImgList = new ArrayList<>();

        // 리뷰 정보 가져오기(이미지 제외)
        List<UserReviewDto> userReviewDto = iUserReviewDaoMapper.selectReviewInfo(a_acc_no);
        log.info("userReviewDto: " + userReviewDto);

        // r_no
        List<Integer> r_nos = iUserReviewDaoMapper.selectReviewRno(a_acc_no);

        for(int i = 0; i < r_nos.size(); i++) {
            int r_no = r_nos.get(i);
            log.info("r_nos~ = {}", r_no);

            // 이미지, 주소 정보 들고 오기
            ResultUserReviewImgList.addAll(iUserReviewDaoMapper.selectReviewImgForList(r_no));
            log.info("r_ri_images: " + ResultUserReviewImgList );

        }

        msgData.put("userReviewDto", userReviewDto);
        msgData.put("userReviewImgList", ResultUserReviewImgList);

        log.info("msgData: " + msgData);

        return msgData;

    }

    // [룸 상세페이지] 리스트 조회
    @Override
    public Map<String, Object> showReviewListRoom(int a_r_no, int a_acc_no) {
        log.info("[userReviewService] showReviewListRoom()");

        Map<String, Object> msgData = new HashMap<>();

        msgData.put("a_r_no", a_r_no);
        msgData.put("a_acc_no", a_acc_no);

        // 리뷰 정보 가져오기(이미지 제외)
        List<UserReviewDto> userReviewDto = iUserReviewDaoMapper.selectReviewInfoInRoom(msgData);
        log.info("userReviewDto: " + userReviewDto);
        // r_no
        List<Integer> r_nos = iUserReviewDaoMapper.selectReviewRnoInRoom(msgData);

        for(int i = 0; i < r_nos.size(); i++) {
            int r_no = r_nos.get(i);

            // 이미지, 주소 정보 들고 오기
            List<UserReviewDto> r_ri_images = iUserReviewDaoMapper.selectReviewImgForList(r_no);
            log.info("r_ri_images: " + r_ri_images );

//            List<UserReviewDto> r_xy_address = iUserReviewDaoMapper.selectReviewAddressForList(r_no);
//            log.info("r_xy_address: " + r_xy_address );

            msgData.put("r_ri_images", r_ri_images);
//            msgData.put("r_xy_address", r_xy_address);

        }
        msgData.put("userReviewDto", userReviewDto);

        log.info("msgData: " + msgData);

        return msgData;

    }

    // [마이페이지] 상세페이지 조회
    @Override
    public Map<String, Object> showDetailMyPage(@RequestParam("r_no") int r_no, @RequestParam("u_m_email") String u_m_email, UserReviewDto userReviewDto) {
        log.info("[userReviewService] showDetail()");

        Map<String ,Object> msgData = new HashMap<>();

        userReviewDto.setR_no(r_no);
        userReviewDto.setU_m_email(u_m_email);

        log.info("r_no:" + userReviewDto.getR_no());
        log.info("r_no:" + userReviewDto.getU_m_email());

        // 리뷰 정보 가져오기(이미지 제외)
        UserReviewDto reviewDto = iUserReviewDaoMapper.selectReviewDetail(userReviewDto);
        log.info("reviewDto:" + reviewDto);

        // front에  u_ri_no 보내기
//        List<Integer> u_ri_nos = iUserReviewDaoMapper.selectReviewImgNo(r_no);
//        log.info("u_ri_nos:" + u_ri_nos);

        // 이미지 정보 가져오기
        List<UserReviewDto> r_ri_images = iUserReviewDaoMapper.selectReviewImgForDetail(r_no);
        log.info("r_ri_images:" + r_ri_images);

        // 주소 정보 가져오기
        List<UserReviewDto> r_xy_address = iUserReviewDaoMapper.selectReviewXYForDetail(r_no);
        log.info("r_xy_address:" + r_xy_address);

        msgData.put("reviewDto", reviewDto);
//        msgData.put("u_ri_nos", u_ri_nos);
        msgData.put("r_ri_images", r_ri_images);
        msgData.put("r_xy_address", r_xy_address);

        log.info("msgData: " + msgData);

        return msgData;

    }

    // [숙박업소] 상세페이지 조회
    @Override
    public Map<String, Object> showDetail(int r_no) throws JsonProcessingException {
        log.info("[userReviewService] showDetail()");

        Map<String ,Object> msgData = new HashMap<>();

        UserReviewDto userReviewDto = new UserReviewDto();

//        log.info("r_no:" + userReviewDto.getR_no());
        log.info("r_no:" + r_no);

        int a_acc_no = iUserReviewDaoMapper.selectReviewAccNo(r_no);
        userReviewDto.setR_no(r_no);
        userReviewDto.setA_acc_no(a_acc_no);

        log.info("r_no: " + userReviewDto.getR_no());
        log.info("a_acc_no: " + userReviewDto.getA_acc_no());

        // 리뷰 정보 가져오기(이미지 제외)
        UserReviewDto reviewDto = iUserReviewDaoMapper.selectReviewDetailForAccm(userReviewDto);
        log.info("reviewDto:" + reviewDto);

        // front에  u_ri_no 보내기
//        List<Integer> u_ri_nos = iUserReviewDaoMapper.selectReviewImgNo(r_no);
//        log.info("u_ri_nos:" + u_ri_nos);

        // 이미지 정보 가져오기
        List<UserReviewDto> r_ri_images = iUserReviewDaoMapper.selectReviewImgForDetail(r_no);
        log.info("r_ri_images:" + r_ri_images);

        // 주소 정보 가져오기
        List<UserReviewDto> r_xy_address = iUserReviewDaoMapper.selectReviewXYForDetail(r_no);
        log.info("r_xy_address:" + r_xy_address);

        // 경도, 위도 바꾸기
        for (int i = 0; i < r_xy_address.size(); i++) {
            String address = r_xy_address.get(i).getR_xy_address();
            
                String jsonString = kakaoMapApiController.getKakaoApiFromAddress(address);

                ObjectMapper mapper = new ObjectMapper();
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>(){};

                Map<String, Object> jsonMap = mapper.readValue(jsonString, typeRef);

                List<LinkedHashMap<String, Object>> document = (List<LinkedHashMap<String, Object>>) jsonMap.get("documents");

                String [] list = String.valueOf(document.get(0).get("address")).split(",");
                for(int j =10; j< list.length; j++) {
                    log.info("list " + j + "" + list[j]);
                }

                String r_xy_longitude = list[10].substring(3, list[10].length());
                String r_xy_latitude = list[11].substring(3, list[11].length()-1);
                userReviewDto.setR_xy_longitude(r_xy_longitude);
                userReviewDto.setR_xy_latitude(r_xy_latitude);

                log.info("x : " + userReviewDto.getR_xy_latitude());
                log.info("x : " + userReviewDto.getR_xy_longitude());

                iUserReviewDaoMapper.insertReviewLoc(userReviewDto);
                reviewDto.setR_xy_latitude(r_xy_latitude);
                reviewDto.setR_xy_longitude(r_xy_longitude);

            }


        msgData.put("reviewDto", reviewDto);
//        msgData.put("u_ri_nos", u_ri_nos);
        msgData.put("r_ri_images", r_ri_images);
        msgData.put("r_xy_address", r_xy_address);

        log.info("msgData: " + msgData);

        return msgData;

    }

    // 삭제
    @Override
    public int deleteConfirm(UserReviewDto userReviewDto) {
        log.info("[userReviewService] deleteConfirm()");

        log.info("r_no:" + userReviewDto.getR_no());
        log.info("u_m_email:" + userReviewDto.getU_m_email());

        // 이미지를 제외한 리뷰 정보 삭제
        int isDeleteInfo = iUserReviewDaoMapper.deleteReview(userReviewDto);
        log.info("isDeleteInfo:" + isDeleteInfo);

        // 삭제할 이미지들 조회
        List<String> deleteImg = iUserReviewDaoMapper.selectReviewImg(userReviewDto);

        // S3에서 이미지 삭제
        for(String imgUrl : deleteImg) {
            s3Uploader.deleteFileFromS3(imgUrl);

        }

        log.info("DELETE REVIEW IMAGES");

        // db에서 이미지 삭제
        int isDeleteImg = iUserReviewDaoMapper.deleteReviewImg(userReviewDto);
        log.info("isDeleteImg: " + isDeleteImg);

        // 삭제할 주소 정보 조회
        List<String> deleteAddress = iUserReviewDaoMapper.selectReviewAddress(userReviewDto);
        log.info("deleteAddress: " + deleteAddress);

        // 경도, 위도 삭제
        int isDeleteAddress = iUserReviewDaoMapper.deleteReviewAddress(userReviewDto);
        log.info("isDeleteAddress: " + isDeleteAddress);

        return isDeleteAddress;

    }
}
