package shc.web.cluborche.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import shc.web.cluborche.dto.*;
import shc.web.cluborche.exception.KafkaFailException;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static shc.web.cluborche.util.OrchestrationBase.*;

@RequiredArgsConstructor
@Component
public class OrchestrationServiceTemplate {
    private final OrchestrationManager orchestrationManager;
    public CommonRsp execute(CommonReq req, OrchestrationCallback callback) {
        CommonRsp rsp = CommonRsp.builder().gid(req.getGid()).build();
        Map<String, BlockingQueue<String>> requestQueueMap = InitiateSAGA(req.getGid());
        try {
            callback.run(requestQueueMap);
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
