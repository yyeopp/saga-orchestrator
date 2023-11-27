package shc.web.cluborche.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubJoinReq {
    private String gid;
    private String clientId;
    private Map<String, Object> data;
}
