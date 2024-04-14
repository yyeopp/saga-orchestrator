package shc.web.cluborche.util;

import shc.web.cluborche.exception.KafkaFailException;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

public interface OrchestrationCallback {

    void run(Map<String, BlockingQueue<String>> requestQueueMap ) throws KafkaFailException;
}
