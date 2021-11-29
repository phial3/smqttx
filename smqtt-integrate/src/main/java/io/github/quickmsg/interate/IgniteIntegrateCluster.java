package io.github.quickmsg.interate;

import io.github.quickmsg.common.event.EventSubscriber;
import io.github.quickmsg.common.event.acceptor.PublishEvent;
import io.github.quickmsg.common.integrate.SubscribeTopic;
import io.github.quickmsg.common.integrate.Integrate;
import io.github.quickmsg.common.integrate.cluster.IntegrateCluster;
import io.github.quickmsg.common.integrate.topic.IntergrateTopics;
import io.github.quickmsg.common.utils.RetryFailureHandler;
import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.lang.IgniteBiPredicate;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author luxurong
 */
public class IgniteIntegrateCluster extends EventSubscriber<PublishEvent> implements IntegrateCluster {

    private final IgniteIntegrate igniteIntegrate;

    private final IgniteMessaging message;

    private final Sinks.Many<PublishEvent> heapMqttMessageMany = Sinks.many().multicast().onBackpressureBuffer();

    private final org.apache.ignite.IgniteCluster igniteCluster;

    public IgniteIntegrateCluster(IgniteIntegrate igniteIntegrate, org.apache.ignite.IgniteCluster igniteCluster) {
        super(igniteIntegrate.getPipeline(), PublishEvent.class);
        this.igniteIntegrate = igniteIntegrate;
        this.message = igniteIntegrate.getIgnite().message();
        this.igniteCluster = igniteCluster;
        message.localListen(this.getLocalNode(), (IgniteBiPredicate<UUID, Object>) this::doRemote);
    }


    @Override
    public Set<String> getClusterNode() {
        return igniteCluster
                .nodes()
                .stream()
                .map(clusterNode -> clusterNode.consistentId().toString())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getOtherClusterNode() {
        return igniteCluster
                .nodes()
                .stream()
                .filter(clusterNode -> clusterNode != igniteCluster.localNode())
                .map(clusterNode -> clusterNode.consistentId().toString())
                .collect(Collectors.toSet());
    }

    @Override
    public String getLocalNode() {
        return igniteIntegrate.getIgnite().cluster().localNode().consistentId().toString();
    }


    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() ->
                message.remoteListen(igniteCluster.localNode().id().toString(), (IgniteBiPredicate<UUID, Object>) (uuid, o) -> true));
    }

    private boolean doRemote(UUID uuid, Object o) {
        heapMqttMessageMany.emitNext((PublishEvent) o, new RetryFailureHandler());
        return true;
    }

    @Override
    public Integrate getIntegrate() {
        return this.igniteIntegrate;
    }

    @Override
    public void apply(PublishEvent publishEvent) {
        IntergrateTopics<SubscribeTopic> topics = igniteIntegrate.getTopics();
        String topic = publishEvent.getTopic();
        Set<String> otherNodes = topics.isWildcard(topic) ?
                this.getOtherClusterNode() : topics.getRemoteTopicsContext(topic);
        otherNodes.forEach(node -> message.send(node, publishEvent));
    }


}
