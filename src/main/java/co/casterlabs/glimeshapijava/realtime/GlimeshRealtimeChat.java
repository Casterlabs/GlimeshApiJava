package co.casterlabs.glimeshapijava.realtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import co.casterlabs.glimeshapijava.GlimeshApiJava;
import co.casterlabs.glimeshapijava.GlimeshAuth;
import co.casterlabs.glimeshapijava.ThreadHelper;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshChatMessage;
import lombok.NonNull;
import lombok.Setter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class GlimeshRealtimeChat implements Closeable {
    private static final String SUBSCRIBE_CHAT = "subscription{chatMessage(channelId: %d){" + GlimeshChatMessage.GQL_DATA + "}}";

    private @Setter @Nullable GlimeshChatListener listener;

    private GlimeshAuth auth;
    private int channelId;

    private FastLogger logger;
    private Connection conn;

    public GlimeshRealtimeChat(@NonNull GlimeshAuth auth, @NonNull GlimeshChannel channel) {
        this(auth, channel.getId());
    }

    @Deprecated
    public GlimeshRealtimeChat(@NonNull GlimeshAuth auth, int channelId) {
        this.auth = auth;
        this.channelId = channelId;

        this.logger = new FastLogger("GlimeshRealtimeChat: " + channelId);
        this.conn = new Connection();
    }

    public void connect() {
        if (!this.isOpen()) {
            if (this.conn.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
                this.conn.connect();
            } else {
                this.conn.reconnect();
            }
        }
    }

    public void connectBlocking() throws InterruptedException {
        if (!this.isOpen()) {
            if (this.conn.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
                this.conn.connectBlocking();
            } else {
                this.conn.reconnectBlocking();
            }
        }
    }

    public boolean isOpen() {
        return this.conn.isOpen();
    }

    public void disconnect() {
        if (this.isOpen()) {
            this.conn.close();
        }
    }

    public void disconnectBlocking() throws InterruptedException {
        if (this.isOpen()) {
            this.conn.closeBlocking();
        }
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

            String subscribe = String.format(SUBSCRIBE_CHAT, channelId);

            this.send(String.format(GlimeshRealtimeHelper.PHX_DOC, GlimeshApiJava.formatQuery(subscribe)));

            ThreadHelper.executeAsync("GlimeshRealtimeChat KeepAlive: " + channelId, () -> {
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
                        GlimeshChatMessage chat = GlimeshApiJava.GSON.fromJson(payload.getAsJsonObject("result").getAsJsonObject("data").get("chatMessage"), GlimeshChatMessage.class);

                        listener.onChat(chat);
                    }
                    break;
                }
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (listener != null) {
                listener.onClose(remote);
            }
        }

        @Override
        public void onError(Exception e) {
            logger.exception(e);
        }

    }

    @Override
    public void close() throws IOException {
        try {
            this.disconnectBlocking();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

}
