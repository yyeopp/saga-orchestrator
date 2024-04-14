package shc.web.cluborche.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shc.web.cluborche.dto.ClubJoinReq;
import shc.web.cluborche.dto.ClubJoinRsp;
import shc.web.cluborche.dto.KafkaKeyDto;
import shc.web.cluborche.dto.KafkaOrchestrationDto;
import shc.web.cluborche.util.OrchestrationManager;
import shc.web.cluborche.util.OrchestrationServiceTemplate;


@Service
@RequiredArgsConstructor
@Slf4j
public class ClubJoinService {

    private final OrchestrationManager orchestrationManager;
    private final OrchestrationServiceTemplate template;

    public ClubJoinRsp join(ClubJoinReq req) {

        return (ClubJoinRsp) template.execute(req, requestQueueMap -> {
            orchestrationManager.sendKafkaEventAndWait(
                    req.getGid(),
                    requestQueueMap,
                    KafkaOrchestrationDto.builder().topic("shc.club.join").key("REQUEST")
                            .kafkaKeyDto(KafkaKeyDto.builder().gid(req.getGid())
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
                                    .build())
                            .build());

            orchestrationManager.sendKafkaEventAndPass(
                    req.getGid(),
                    KafkaOrchestrationDto.builder().topic("shc.sms").key("REQUEST")
                            .kafkaKeyDto(KafkaKeyDto.builder().gid(req.getGid())
                                    .hwnName((String) req.getData().get("hwnName"))
                                    .phone((String) req.getData().get("phone"))
                                    .build())
                            .build());
        });

    }
}
