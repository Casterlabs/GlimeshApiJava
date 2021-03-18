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
import co.casterlabs.glimeshapijava.types.GlimeshUser;
import lombok.NonNull;
import lombok.Setter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class GlimeshRealtimeFollowers implements Closeable {
    private static final String SUBSCRIBE_CHANNEL = "subscription{followers(streamerId: %d){user{" + GlimeshUser.GQL_DATA + "}}}";

    private @Setter @Nullable GlimeshFollowerListener listener;

    private GlimeshAuth auth;
    private int userId;

    private FastLogger logger;
    private Connection conn;

    public GlimeshRealtimeFollowers(@NonNull GlimeshAuth auth, @NonNull GlimeshUser user) {
        this(auth, user.getId());
    }

    @Deprecated
    public GlimeshRealtimeFollowers(@NonNull GlimeshAuth auth, int userId) {
        this.auth = auth;
        this.userId = userId;

        this.logger = new FastLogger("GlimeshRealtimeFollowers: " + userId);
        this.conn = new Connection();
    }

    public void connect() {
        if (this.conn.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
            this.conn.connect();
        } else {
            this.conn.reconnect();
        }
    }

    public void connectBlocking() throws InterruptedException {
        if (this.conn.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
            this.conn.connectBlocking();
        } else {
            this.conn.reconnectBlocking();
        }
    }

    public void disconnect() {
        this.conn.close();
    }

    public void disconnectBlocking() throws InterruptedException {
        this.conn.closeBlocking();
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

            String subscribe = String.format(SUBSCRIBE_CHANNEL, userId);

            this.send(String.format(GlimeshRealtimeHelper.PHX_DOC, GlimeshApiJava.formatQuery(subscribe)));

            ThreadHelper.executeAsync("GlimeshRealtimeFollowers KeepAlive: " + userId, () -> {
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
                        GlimeshUser follower = GlimeshApiJava.GSON.fromJson(payload.getAsJsonObject("result").getAsJsonObject("data").getAsJsonObject("followers").get("user"), GlimeshUser.class);

                        listener.onFollow(follower);
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
