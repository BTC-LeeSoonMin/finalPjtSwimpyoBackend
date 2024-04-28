package com.btc.swimpyo.backend.controller.accm.user;

import com.btc.swimpyo.backend.dto.accm.admin.AdminAccmDto;
import com.btc.swimpyo.backend.service.accm.user.ISearchAccmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/user/accm")
@RequiredArgsConstructor
public class SearchAccmController {

    private final ISearchAccmService searchAccmService;

    @PostMapping("/search")
    public Object searchAccm(@RequestBody Map<String, Object> msgMap, AdminAccmDto adminAccmDto) {
        log.info("searchAccm");

//        List<AdminAccmDto> searchAccms = searchAccmService.searchAccm(msgMap, adminAccmDto);
        List<Map<String, Object>> searchAccms = searchAccmService.searchAccm(msgMap, adminAccmDto);

        log.info("searchAccms ==> {}", searchAccms);

        return searchAccms;
    }

    @GetMapping("/mapInfoList")
    public Object mapInfoList(@RequestParam ("region") String region){
        log.info("mapInfoList");

        List<AdminAccmDto> mapInfoList = searchAccmService.mapInfoList(region);
        if(mapInfoList == null){
            return "emptyMapInfo";
        }
        return mapInfoList;
    }

    @GetMapping("/rankAccmList")
    public Object rankAccmList(@RequestParam ("accmValue") String accmValue){
        log.info("rankAccmList");
        log.info("accmValue ==> {}", accmValue);

        return searchAccmService.rankAccmList(accmValue);
    }
}
