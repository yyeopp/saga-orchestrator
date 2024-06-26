package shc.web.cluborche.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonRsp {
    private String gid;
    private String clientId;
    private String code;
    private String message;
    private Object data;
}
