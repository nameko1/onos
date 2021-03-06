/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.primitives.impl;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.protocol.CloseSessionRequest;
import io.atomix.protocols.raft.protocol.CloseSessionResponse;
import io.atomix.protocols.raft.protocol.CommandRequest;
import io.atomix.protocols.raft.protocol.CommandResponse;
import io.atomix.protocols.raft.protocol.HeartbeatRequest;
import io.atomix.protocols.raft.protocol.HeartbeatResponse;
import io.atomix.protocols.raft.protocol.KeepAliveRequest;
import io.atomix.protocols.raft.protocol.KeepAliveResponse;
import io.atomix.protocols.raft.protocol.MetadataRequest;
import io.atomix.protocols.raft.protocol.MetadataResponse;
import io.atomix.protocols.raft.protocol.OpenSessionRequest;
import io.atomix.protocols.raft.protocol.OpenSessionResponse;
import io.atomix.protocols.raft.protocol.PublishRequest;
import io.atomix.protocols.raft.protocol.QueryRequest;
import io.atomix.protocols.raft.protocol.QueryResponse;
import io.atomix.protocols.raft.protocol.RaftClientProtocol;
import io.atomix.protocols.raft.protocol.ResetRequest;
import io.atomix.protocols.raft.session.SessionId;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.service.Serializer;

/**
 * Raft client protocol that uses a cluster communicator.
 */
public class RaftClientCommunicator extends RaftCommunicator implements RaftClientProtocol {

    public RaftClientCommunicator(
            String prefix,
            Serializer serializer,
            ClusterCommunicationService clusterCommunicator) {
        super(new RaftMessageContext(prefix), serializer, clusterCommunicator);
    }

    @Override
    public CompletableFuture<OpenSessionResponse> openSession(MemberId memberId, OpenSessionRequest request) {
        return sendAndReceive(context.openSessionSubject, request, memberId);
    }

    @Override
    public CompletableFuture<CloseSessionResponse> closeSession(MemberId memberId, CloseSessionRequest request) {
        return sendAndReceive(context.closeSessionSubject, request, memberId);
    }

    @Override
    public CompletableFuture<KeepAliveResponse> keepAlive(MemberId memberId, KeepAliveRequest request) {
        return sendAndReceive(context.keepAliveSubject, request, memberId);
    }

    @Override
    public CompletableFuture<QueryResponse> query(MemberId memberId, QueryRequest request) {
        return sendAndReceive(context.querySubject, request, memberId);
    }

    @Override
    public CompletableFuture<CommandResponse> command(MemberId memberId, CommandRequest request) {
        return sendAndReceive(context.commandSubject, request, memberId);
    }

    @Override
    public CompletableFuture<MetadataResponse> metadata(MemberId memberId, MetadataRequest request) {
        return sendAndReceive(context.metadataSubject, request, memberId);
    }

    @Override
    public void registerHeartbeatHandler(Function<HeartbeatRequest, CompletableFuture<HeartbeatResponse>> function) {
        clusterCommunicator.addSubscriber(context.heartbeatSubject, serializer::decode, function, serializer::encode);
    }

    @Override
    public void unregisterHeartbeatHandler() {
        clusterCommunicator.removeSubscriber(context.heartbeatSubject);
    }

    @Override
    public void reset(Collection<MemberId> members, ResetRequest request) {
        Set<NodeId> nodes = members.stream().map(m -> NodeId.nodeId(m.id())).collect(Collectors.toSet());
        clusterCommunicator.multicast(
                request,
                context.resetSubject(request.session()),
                serializer::encode,
                nodes);
    }

    @Override
    public void registerPublishListener(SessionId sessionId, Consumer<PublishRequest> listener, Executor executor) {
        clusterCommunicator.addSubscriber(
                context.publishSubject(sessionId.id()),
                serializer::decode,
                listener,
                executor);
    }

    @Override
    public void unregisterPublishListener(SessionId sessionId) {
        clusterCommunicator.removeSubscriber(context.publishSubject(sessionId.id()));
    }
}
