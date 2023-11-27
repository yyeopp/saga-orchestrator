package shc.web.cluborche.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaOrchestrationDto {
    private String topic;
    private String key;
    private KafkaKeyDto kafkaKeyDto;
}
