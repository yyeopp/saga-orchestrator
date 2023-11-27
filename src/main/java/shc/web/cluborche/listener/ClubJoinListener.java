package shc.web.cluborche.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import shc.web.cluborche.dto.KafkaKeyDto;

import static shc.web.cluborche.util.OrchestrationBase.PushSingleBlockingQueue;

@Component
@Slf4j
public class ClubJoinListener {

    @KafkaListener(topics = "shc.club.join.result", groupId = "shc", containerFactory = "clubKafkaListenerContainerFactory")
    public void listenJoin(
            @Payload KafkaKeyDto kafkaKeyDTO,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) throws InterruptedException {
        PushSingleBlockingQueue(kafkaKeyDTO.getGid(), topic.replaceAll("\\.result$", ""), key);
    }

    @KafkaListener(topics = "shc.pay.result", groupId = "shc", containerFactory = "clubKafkaListenerContainerFactory")
    public void listenPayment(@Payload KafkaKeyDto kafkaKeyDTO,
                              @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) throws InterruptedException {
        PushSingleBlockingQueue(kafkaKeyDTO.getGid(), topic.replaceAll("\\.result$", ""), key);
    }

    @KafkaListener(topics = "shc.ext.payment.result", groupId = "shc", containerFactory = "clubKafkaListenerContainerFactory")
    public void listenExtPayment(@Payload KafkaKeyDto kafkaKeyDTO,
                              @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) throws InterruptedException {
        PushSingleBlockingQueue(kafkaKeyDTO.getGid(), topic.replaceAll("\\.result$", ""), key);
    }

}
