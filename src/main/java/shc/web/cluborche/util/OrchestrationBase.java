package shc.web.cluborche.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import shc.web.cluborche.dto.KafkaOrchestrationDto;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class OrchestrationBase implements ApplicationRunner {

    public static Map<String, Map<String, BlockingQueue<String>>> CLUB_ORCHESTRATOR_BASE;
    public static Map<String, Map<String, KafkaOrchestrationDto>> ORCHESTRATOR_DTO_MEMORY;

    /**
     * SpringBoot Run 시점에, CLUB_ORCHESTRATOR_BASE 와 ORCHESTRATOR_DTO_MEMORY 를 로딩
     *
     * @param args incoming application arguments
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("###################################");
        log.info("##### Club Orchestrator START #####");
        log.info("###################################");
        CLUB_ORCHESTRATOR_BASE = new ConcurrentHashMap<>();
        ORCHESTRATOR_DTO_MEMORY = new ConcurrentHashMap<>();
        log.info(">>>>>>>>>>>>> Global Blocking Queue TABLE = {}", CLUB_ORCHESTRATOR_BASE.hashCode());
        log.info(">>>>>>>>>>>>> Global Orchestration DTO TABLE = {}", CLUB_ORCHESTRATOR_BASE.hashCode());
    }

    /**
     * SAGA 오케스트레이션 시작
     *
     * @param GID
     * @return Map<String, BlockingQueue < String>> requestQueueMap
     */
    public static Map<String, BlockingQueue<String>> InitiateSAGA(String GID) {
        Map<String, BlockingQueue<String>> requestQueueMap = new HashMap<>();
        Map<String, KafkaOrchestrationDto> orchestrationDtoMap = new HashMap<>();

        CLUB_ORCHESTRATOR_BASE.put(GID, requestQueueMap);
        ORCHESTRATOR_DTO_MEMORY.put(GID, orchestrationDtoMap);

        log.info(">>>>>>>>>>>> INITIATE SAGA with [GID = {}]", GID);

        return requestQueueMap;
    }

    /**
     * SAGA 오케스트레이션 종료
     *
     * @param Gid
     */
    public static void TerminateSAGA(String GID) {
        log.info(">>>>>>>>>>>> TERMINATE SAGA with [GID = {}]", GID);

        CLUB_ORCHESTRATOR_BASE.remove(GID);
        ORCHESTRATOR_DTO_MEMORY.remove(GID);
    }

    /**
     * 순차처리(Sync) - 특정 GID, Topic 으로 발송한 Kafka 이벤트의 result 가 수신될 때까지 Thread Block
     *
     * @param requestQueueMap
     * @param topic
     * @return String response
     */
    public static String WaitKafkaResponse(Map<String, BlockingQueue<String>> requestQueueMap, String topic) {
        String result = "FAIL";
        try {
            log.info(">>>>>>>>>>>> Blocking Queue WAIT, [topic = {}]", topic);
            result = requestQueueMap.get(topic).take();
            log.info(">>>>>>>>>>>> Blocking Queue RESPONDED, [topic = {}, RESULT = {}]", topic, result);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 순차처리(Sync) - 특정 GID, Topic 으로 발송한 Kafka 이벤트의 result 가 수신되면, 해당 BlockingQueue 에 insert
     *
     * @param GID
     * @param topic
     * @param value
     */
    public static void PushSingleBlockingQueue(String GID, String topic, String value) {
        try {
            CLUB_ORCHESTRATOR_BASE.get(GID).get(topic).put(value);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 보상트랜잭션 - 특정 GID, Topic 으로 발송한 Kafka 이벤트가 성공하면, 사용한 kafkaOrchestrationDto 를 저장
     *
     * @param GID
     * @param kafkaOrchestrationDto
     */
    public static void StoreKafkaDtoMemory(String GID, KafkaOrchestrationDto kafkaOrchestrationDto) {
        ORCHESTRATOR_DTO_MEMORY.get(GID).put(kafkaOrchestrationDto.getTopic(), kafkaOrchestrationDto);
    }

    /**
     * 보상트랜잭션 - 지금까지 사용한 KafkaOrchestrationDto 목록을 GID, Topic 과 매칭하여 로딩
     *
     * @param GID
     * @return Map<String, KafkaOrchestrationDto> orchestrationDtoMap
     */
    public static Map<String, KafkaOrchestrationDto> GetDtoMemory(String GID) {
        return ORCHESTRATOR_DTO_MEMORY.get(GID);
    }

}
