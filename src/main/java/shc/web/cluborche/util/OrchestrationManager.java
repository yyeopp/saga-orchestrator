package shc.web.cluborche.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import shc.web.cluborche.dto.KafkaKeyDto;
import shc.web.cluborche.dto.KafkaOrchestrationDto;
import shc.web.cluborche.exception.KafkaFailException;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static shc.web.cluborche.util.OrchestrationBase.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrchestrationManager {
    private final KafkaTemplate<String, KafkaKeyDto> kafkaTemplate;

    /**
     * 특정 GID, Topic 으로 Kafka event 를 발송하고, 그 응답이 수신될 때까지 대기 / 응답이 FAIL 이면 보상트랜잭션 유발
     *
     * @param gid
     * @param requestQueueMap
     * @param orchestrationDto
     * @throws KafkaFailException
     */
    public void sendKafkaEventAndWait(String gid, Map<String, BlockingQueue<String>> requestQueueMap, KafkaOrchestrationDto orchestrationDto) throws KafkaFailException {
        log.info(">>>>>>>>>> sendKafkaEventAnd WAIT : [TOPIC = {} , KEY = {}]", orchestrationDto.getTopic(), orchestrationDto.getKey());
        requestQueueMap.put(orchestrationDto.getTopic(), new ArrayBlockingQueue<>(1));
        kafkaTemplate.send(orchestrationDto.getTopic(), orchestrationDto.getKey(), orchestrationDto.getKafkaKeyDto());
        String result = WaitKafkaResponse(requestQueueMap, orchestrationDto.getTopic());
        if ("FAIL".equals(result)) {
            log.error(">>>>>>>>>>>>>>> COMPENSATION : [GID = {}]", gid);
            log.error(">>>>>>>>>>>>>>> Caused By : [TOPIC = {}]", orchestrationDto.getTopic());
            throw new KafkaFailException();
        }
        StoreKafkaDtoMemory(gid, orchestrationDto);
    }

    /**
     * 특정 GID, Topic 으로 Kafka event 를 발송하고, 응답 수신 여부 확인 없이 종료
     *
     * @param gid
     * @param orchestrationDto
     */
    public void sendKafkaEventAndPass(String gid, KafkaOrchestrationDto orchestrationDto) {
        log.info(">>>>>>>>>> sendKafkaEventAnd PASS : [TOPIC = {} , KEY = {}]", orchestrationDto.getTopic(), orchestrationDto.getKey());
        kafkaTemplate.send(orchestrationDto.getTopic(), orchestrationDto.getKey(), orchestrationDto.getKafkaKeyDto());
    }

    /**
     * 보상트랜잭션 - 지금까지 사용한 KafkaOrchestrationDto 목록을 로딩한 후, key = FAIL 로 Kakfa 이벤트 발생
     *
     * @param gid
     */
    public void activateCompensation(String gid) {
        log.warn(">>>>>>>>>> COMPENSATE START : [GID = {}]", gid);
        GetDtoMemory(gid).forEach((String topic, KafkaOrchestrationDto kafkaOrchestrationDto) -> {
            log.warn(">>>>>>>>>> COMPENSATE : [TOPIC = {}]", topic);
            sendKafkaCompensation(kafkaOrchestrationDto);
        });
    }

    /**
     * 보상트랜잭션 - key = FAIL 로 Kafka 이벤트를 발생시키고, 성공 여부는 확인하지 않음
     *
     * @param orchestrationDto
     */
    public void sendKafkaCompensation(KafkaOrchestrationDto orchestrationDto) {
        log.warn(">>>>>>>>>> sendKafka COMPENSATION : [TOPIC = {}, KEY = {}]", orchestrationDto.getTopic(), "FAIL");
        kafkaTemplate.send(orchestrationDto.getTopic(), "FAIL", orchestrationDto.getKafkaKeyDto());
    }


}
