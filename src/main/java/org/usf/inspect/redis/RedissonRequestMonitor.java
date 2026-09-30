package org.usf.inspect.redis;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usf.inspect.core.InspectExecutor.ExecutionListener;
import org.usf.inspect.core.Monitor.StatefulMonitor;

import java.net.InetSocketAddress;

import static org.usf.inspect.core.Helper.threadName;
import static org.usf.inspect.core.SessionContextManager.nextId;

public class RedissonRequestMonitor extends StatefulMonitor<RedissonRequestSignal, RedissonRequestUpdate> {

    private static final Logger log = LoggerFactory.getLogger(RedissonRequestMonitor.class);

    @Override
    protected RedissonRequestUpdate createCallback(RedissonRequestSignal session) {
        return session.createCallback();
    }

    public ExecutionListener<Object> sendHandler(ChannelHandlerContext ctx, Object msg) {

        return traceBegin(start -> {
            var id = nextId();
            return new RedissonRequestSignal(id, "test"+id, start, threadName());
        },
        (req, o)->{
            if (msg instanceof ByteBuf buf) {
                var resp = RespParser.parse(buf);
                log.info("[Redisson -> Redis] Id: {} ; commande : {} ; args : {}",
                        req.getId(), resp.command(), java.util.Arrays.toString(resp.args()));
            }
            var addr = ctx.channel().remoteAddress();
            if (addr instanceof InetSocketAddress inetAddr) {
                String host = inetAddr.getHostString(); // pas de résolution DNS contrairement à getHostName()
                int port = inetAddr.getPort();
                req.setPort(port);
                req.setHost(host);
            }
            else {
                req.setPort(-1);
                req.setHost(addr.toString());
            }

        }, stageHandler(RedissonAction.SEND, null));
    }

    public ExecutionListener<Object> responseHandler(Object msg) {

        if (msg instanceof ByteBuf buf) {
            var reply = RespParser.parseResponse(buf);
            log.info("[Redis -> Redisson] réponse : {}", reply);
        }
        return traceEnd(stageHandler(RedissonAction.RECEIVE, null));
    }

    <T> ExecutionListener<T> stageHandler(RedissonAction action, RedisCommand cmd, String... args) {
        return traceStep((s,e,o,t)-> getCallback().createStage(action, s, e, t, cmd, args));
    }
}
