package shc.web.cluborche.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import shc.web.cluborche.dto.KafkaKeyDto;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${coc.kafka.node1}")
    private String kafkaNode1;
    @Value("${coc.kafka.node2}")
    private String kafkaNode2;
    @Value("${coc.kafka.node3}")
    private String kafkaNode3;

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.format("%s,%s,%s", kafkaNode1, kafkaNode2, kafkaNode3));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "shc");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, true);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }

    @Bean
    public ConsumerFactory<String, KafkaKeyDto> clubConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(),
                new JsonDeserializer<>(KafkaKeyDto.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, KafkaKeyDto> clubKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, KafkaKeyDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(clubConsumerFactory());
        return factory;
    }

}
