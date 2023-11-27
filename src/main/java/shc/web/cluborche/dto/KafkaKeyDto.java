package shc.web.cluborche.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaKeyDto {
    private String gid;
    private String startTime;
    private String method;
    private String caller;
    private String etc;
    private Long clubId;
    private Long memberId;
    private Long dues;
    private String hwnName;
    private String phone;
}