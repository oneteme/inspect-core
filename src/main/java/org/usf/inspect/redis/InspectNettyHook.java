package org.usf.inspect.redis;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.redisson.client.NettyHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

import static org.usf.inspect.core.InspectExecutor.exec;

public class InspectNettyHook implements NettyHook {

    @Override
    public void afterBoostrapInitialization(Bootstrap bootstrap) {
    }

    @Override
    public void afterChannelInitialization(Channel channel) {
        channel.pipeline().addFirst("redisson-interceptor", new RedissonRequestInterceptor());
        //channel.pipeline().addLast("redisson-interceptor", new RedissonRequestInterceptor());
    }
}

class RedissonRequestInterceptor extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(RedissonRequestInterceptor.class);
    private RedissonRequestMonitor redissonRequestMonitor;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Intercepte les requêtes sortantes Redisson -> Redis
        redissonRequestMonitor = new RedissonRequestMonitor();
        exec(() -> super.write(ctx, msg, promise), redissonRequestMonitor.sendHandler(ctx, msg));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Intercepte les réponses entrantes Redis -> Redisson
        exec(() -> super.channelRead(ctx, msg), redissonRequestMonitor.responseHandler(msg));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Erreur sur le canal Redis", cause);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        log.info("[Redisson -> Redis] connect : {}", remoteAddress.toString());
        super.connect(ctx, remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        log.info("[Redisson -> Redis] Disconnect : {}", promise.toString());
        super.disconnect(ctx, promise);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        log.info("[Redisson -> Redis] Bind : {}", localAddress.toString());
        super.bind(ctx, localAddress, promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        log.info("[Redisson -> Redis] Close : {}", promise.toString());
        super.close(ctx, promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        log.info("[Redisson -> Redis] Deregister : {}", promise.toString());
        super.deregister(ctx, promise);
    }
}