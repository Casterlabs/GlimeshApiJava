package co.casterlabs.glimeshapijava.realtime;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import co.casterlabs.glimeshapijava.GlimeshApiJava;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.glimeshapijava.ThreadHelper;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import lombok.NonNull;
import lombok.Setter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class GlimeshRealtimeChannel implements Closeable {
    private static final String SUBSCRIBE_CHANNEL = "subscription{channel(id: %d){" + GlimeshChannel.GQL_DATA + "}}";

    private @Setter @Nullable GlimeshChannelListener listener;

    private GlimeshAuth auth;
    private int channelId;

    private FastLogger logger;
    private Connection conn;

    public GlimeshRealtimeChannel(@NonNull GlimeshAuth auth, @NonNull GlimeshChannel channel) {
        this(auth, channel.getId());
    }

    @Deprecated
    public GlimeshRealtimeChannel(@NonNull GlimeshAuth auth, int channelId) {
        this.auth = auth;
        this.channelId = channelId;

        this.logger = new FastLogger("GlimeshRealtimeChannel: " + channelId);
        this.conn = new Connection();
    }

    public void connect() {
        if (this.conn == null) {
            this.conn = new Connection();
            this.conn.connect();
        } else {
            throw new IllegalStateException("You must close the connection before reconnecting.");
        }
    }

    public void connectBlocking() throws InterruptedException {
        if (this.conn == null) {
            this.conn = new Connection();
            this.conn.connectBlocking();
        } else {
            throw new IllegalStateException("You must close the connection before reconnecting.");
        }
    }

    public boolean isOpen() {
        return this.conn != null;
    }

    @Override
    public void close() {
        this.conn.close();
    }

    private class Connection extends WebSocketClient {

        public Connection() {
            super(auth.getRealtimeUrl());
        }

        @Override
        public void send(String payload) {
            logger.debug(GlimeshRealtimeHelper.DEBUG_WS_SEND, payload);
            super.send(payload);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            this.send(GlimeshRealtimeHelper.PHX_JOIN);

            String subscribe = String.format(SUBSCRIBE_CHANNEL, channelId);

            this.send(String.format(GlimeshRealtimeHelper.PHX_DOC, GlimeshApiJava.formatQuery(subscribe)));

            ThreadHelper.executeAsync("GlimeshRealtimeChannel KeepAlive: " + channelId, () -> {
                while (this.isOpen()) {
                    this.send(GlimeshRealtimeHelper.PHX_KA);
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    } catch (InterruptedException ignored) {}
                }
            });

            if (listener != null) {
                listener.onOpen();
            }
        }

        @Override
        public void onMessage(String raw) {
            logger.debug(GlimeshRealtimeHelper.DEBUG_WS_RECIEVE, raw);

            JsonArray phx = GlimeshApiJava.GSON.fromJson(raw, JsonArray.class);

            String type = phx.get(3).getAsString();
            JsonObject payload = phx.get(4).getAsJsonObject();

            switch (type) {
                case "phx_reply": {
                    if (!payload.get("status").getAsString().equals("ok")) {
                        this.close();
                    }

                    break;
                }

                case "subscription:data": {
                    if (listener != null) {
                        GlimeshChannel channel = GlimeshApiJava.GSON.fromJson(payload.getAsJsonObject("result").getAsJsonObject("data").get("channel"), GlimeshChannel.class);

                        listener.onUpdate(channel);
                    }
                    break;
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            conn = null;

            if (listener != null) {
                listener.onClose(remote);
            }
        }

        @Override
        public void onError(Exception e) {
            logger.exception(e);
        }

    }

}
