package shc.web.cluborche.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shc.web.cluborche.dto.ClubJoinReq;
import shc.web.cluborche.dto.ClubJoinRsp;
import shc.web.cluborche.service.ClubJoinService;

import java.util.UUID;

@RestController
@RequestMapping("/orch-club")
@RequiredArgsConstructor
@Slf4j
public class ClubController {
    private final ClubJoinService clubJoinService;
    private final ObjectMapper objectMapper;

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody ClubJoinReq req) {
        log.info(req.toString());
        req.setGid(UUID.randomUUID().toString());

        ClubJoinRsp rsp = clubJoinService.join(req);
        log.info(rsp.toString());

        if("500".equals(rsp.getCode())) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok()
                .body(ClubJoinRsp.builder().code("200").message("OK")
                        .data(rsp).build());
    }


}
