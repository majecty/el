package io.el.channel;

import io.el.concurrent.EventLoop;
import io.el.internal.ObjectUtil;
import java.net.SocketAddress;

/**
 * {@link DefaultChannelPipeline} manages a channel handler list. When an event occurs, it calls the
 * handlers it manages.
 */
public class DefaultChannelPipeline implements ChannelPipeline {

  private final Channel channel;
  private final HeadContext headContext;
  private final TailContext tailContext;

  public DefaultChannelPipeline(Channel channel) {
    this.channel = channel;
    this.tailContext = new TailContext();
    this.headContext = new HeadContext();

    this.tailContext.prev = this.headContext;
    this.headContext.next = this.tailContext;
  }

  @Override
  public Channel channel() {
    return this.channel;
  }

  @Override
  public ChannelPipeline addLast(ChannelHandler... handlers) {
    for (ChannelHandler handler : handlers) {
      this.addLast(handler);
    }
    return this;
  }

  private ChannelPipeline addLast(ChannelHandler handler) {
    final AbstractChannelHandlerContext__ prev = this.tailContext.prev;
    final AbstractChannelHandlerContext__ newHandlerContext = new DefaultHandlerContext__(handler);

    this.tailContext.prev = newHandlerContext;
    newHandlerContext.next = this.tailContext;

    prev.next = newHandlerContext;
    newHandlerContext.prev = prev;
    return this;
  }

  @Override
  public ChannelPipeline remove(ChannelHandler handler) {
    final AbstractChannelHandlerContext__ context = this.context(handler);
    remove(context);
    return this;
  }

  private void remove(AbstractChannelHandlerContext__ context) {
    assert context != this.headContext && context != this.tailContext;

    // We need synchronize to call handler removed event atomically.
    synchronized (this) {
      atomicRemoveFromHeandlerList(context);

      // TODO: call handler removed
    }
  }

  /**
   * By using this, we can update the next and the prev reference atomically.
   */
  private synchronized void atomicRemoveFromHeandlerList(AbstractChannelHandlerContext__ context) {
    final AbstractChannelHandlerContext__ prev = context.prev;
    final AbstractChannelHandlerContext__ next = context.next;
    prev.next = next;
    next.prev = prev;
  }

  @Override
  public AbstractChannelHandlerContext__ context(ChannelHandler handler) {
    ObjectUtil.checkNotNull(handler, "handler");

    AbstractChannelHandlerContext__ ctx = headContext.next;
    for (;;) {
      if (ctx == null) {
        return null;
      }

      if (ctx.handler() == handler) {
        return ctx;
      }

      ctx = ctx.next;
    }
  }

  @Override
  public ChannelHandlerContext firstContext() {
    final AbstractChannelHandlerContext__ next = this.headContext.next;
    if (next == this.tailContext) {
      return null;
    }
    return next;
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress) {
    return this.tailContext.bind(localAddress);
  }

  @Override
  public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
    return this.tailContext.bind(localAddress, promise);
  }

  @Override
  public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
      ChannelPromise promise) {
    return this.tailContext.bind(remoteAddress, localAddress, promise);
  }

  @Override
  public ChannelPipeline fireChannelRegistered() {
   return null;
  }

  /**
   * Temp class instead of AbstractChannelHandlerContext
   */
  private static class AbstractChannelHandlerContext__ implements ChannelHandlerContext {

    public AbstractChannelHandlerContext__ next;
    public AbstractChannelHandlerContext__ prev;

    public AbstractChannelHandlerContext__() {
    }

    @Override
    public Channel channel() {
      return null;
    }

    @Override
    public EventLoop eventLoop() {
      return null;
    }

    @Override
    public ChannelHandlerContext fireChannelRegistered() {
      return null;
    }

    @Override
    public ChannelPipeline pipeline() {
      return null;
    }

    @Override
    public ChannelHandler handler() {
      return null;
    }

    @Override
    public ChannelPromise bind(SocketAddress localAddress) {
      return null;
    }

    @Override
    public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
      return null;
    }

    @Override
    public ChannelPromise connect(SocketAddress remoteAddress, SocketAddress localAddress,
        ChannelPromise promise) {
      return null;
    }
  }

  private static final class DefaultHandlerContext__ extends AbstractChannelHandlerContext__ {

    private final ChannelHandler handler;

    public DefaultHandlerContext__(ChannelHandler handler) {
     this.handler = handler;
    }

    @Override
    public ChannelHandler handler() {
      return this.handler;
    }
  }

  /**
   * Todo: This class will extends AbstractChannelHandlerContext
   * The first handler context.
   *
   * For the events which calls handlers from last,
   * the {@link HeadContext} will call channel's methods
   * after all the handers are called.
   */
  private static final class HeadContext extends AbstractChannelHandlerContext__ {

    private final HeadContextHandler context;

    public HeadContext() {
      this.context = new HeadContextHandler();
    }

    @Override
    public ChannelHandler handler() {
      return this.context;
    }

    /**
     * Pipeline will call `bind` from the tail handler. This is the last `bind` function in the pipe
     * line.
     */
    @Override
    public ChannelPromise bind(SocketAddress localAddress, ChannelPromise promise) {
      this.channel().internal().bind(localAddress, promise);
      // FIXME: I'm not sure whether returning the promise from argument is ok or not
      return promise;
    }
  }

  private static final class HeadContextHandler implements ChannelHandler {
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      // TODO:
    }
  }

  private static final class TailContext extends AbstractChannelHandlerContext__ {

    private final TailContextHandler context;

    public TailContext() {
      this.context = new TailContextHandler();
    }

    @Override
    public ChannelHandler handler() {
      return this.context;
    }
  }

  /**
   * The last handler.
   */
  private static final class TailContextHandler implements ChannelHandler {
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      // TODO:
    }
  }
}
