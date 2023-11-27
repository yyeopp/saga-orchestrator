package shc.web.cluborche.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shc.web.cluborche.dto.ClubJoinReq;
import shc.web.cluborche.dto.ClubJoinRsp;
import shc.web.cluborche.dto.KafkaKeyDto;
import shc.web.cluborche.dto.KafkaOrchestrationDto;
import shc.web.cluborche.exception.KafkaFailException;
import shc.web.cluborche.util.OrchestrationManager;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static shc.web.cluborche.util.OrchestrationBase.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class ClubJoinService {

    private final OrchestrationManager orchestrationManager;

    public ClubJoinRsp join(ClubJoinReq req) {
        ClubJoinRsp rsp = ClubJoinRsp.builder().gid(req.getGid()).build();
        Map<String, BlockingQueue<String>> requestQueueMap = InitiateSAGA(req.getGid());
        try {
            orchestrationManager.sendKafkaEventAndWait(
                    req.getGid(),
                    requestQueueMap,
                    KafkaOrchestrationDto.builder().topic("shc.club.join").key("REQUEST")
                            .kafkaKeyDto(KafkaKeyDto.builder().gid(req.getGid())
                                    .method("REQUEST")
                                    .clubId(Long.parseLong((String) req.getData().get("clubId")))
                                    .hwnName((String) req.getData().get("hwnName"))
                                    .phone((String) req.getData().get("phone"))
                                    .build())
                            .build());

            orchestrationManager.sendKafkaEventAndWait(
                    req.getGid(),
                    requestQueueMap,
                    KafkaOrchestrationDto.builder().topic("shc.pay").key("REQUEST")
                            .kafkaKeyDto(KafkaKeyDto.builder().gid(req.getGid())
                                    .method("REQUEST")
                                    .clubId(Long.parseLong((String) req.getData().get("clubId")))
                                    .dues(Long.parseLong((String) req.getData().get("dues")))
                                    .hwnName((String) req.getData().get("hwnName"))
                                    .build())
                            .build());

            orchestrationManager.sendKafkaEventAndWait(
                    req.getGid(),
                    requestQueueMap,
                    KafkaOrchestrationDto.builder().topic("shc.ext.payment").key("REQUEST")
                            .kafkaKeyDto(KafkaKeyDto.builder().gid(req.getGid())
                                    .method("REQUEST")
                                    .build())
                            .build());

            orchestrationManager.sendKafkaEventAndPass(
                    req.getGid(),
                    KafkaOrchestrationDto.builder().topic("shc.sms").key("SUCCESS")
                            .kafkaKeyDto(KafkaKeyDto.builder().gid(req.getGid())
                                    .method("REQUEST")
                                    .hwnName((String) req.getData().get("hwnName"))
                                    .phone((String) req.getData().get("phone"))
                                    .build())
                            .build());

            rsp.setCode("200");
        } catch (KafkaFailException e) {
            orchestrationManager.activateCompensation(req.getGid());
            rsp.setCode("500");
        } finally {
            TerminateSAGA(req.getGid());
        }
        return rsp;
    }

}
