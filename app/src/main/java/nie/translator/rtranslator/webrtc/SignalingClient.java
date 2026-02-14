package nie.translator.rtranslator.webrtc;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class SignalingClient {
    private static final String TAG = "SignalingClient";
    private WebSocketClient socket;
    private final String serverUrl;
    private final String roomId;
    private final SignalingInterface callback;

    public interface SignalingInterface {
        void onOfferReceived(JSONObject data);
        void onAnswerReceived(JSONObject data);
        void onIceCandidateReceived(JSONObject data);
        void onPeerJoined();
        void onError(String message);
    }

    public SignalingClient(String serverUrl, String roomId, SignalingInterface callback) {
        this.serverUrl = serverUrl;
        this.roomId = roomId;
        this.callback = callback;
    }

    public void connect() {
        try {
            URI uri = new URI(serverUrl);
            socket = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "Connected to Signaling Server");
                    // Join Room
                    JSONObject joinObject = new JSONObject();
                    try {
                        joinObject.put("type", "join");
                        joinObject.put("room", roomId);
                        socket.send(joinObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject object = new JSONObject(message);
                        String type = object.getString("type");
                        
                        if (type.equalsIgnoreCase("offer")) {
                            callback.onOfferReceived(object);
                        } else if (type.equalsIgnoreCase("answer")) {
                            callback.onAnswerReceived(object);
                        } else if (type.equalsIgnoreCase("candidate")) {
                            callback.onIceCandidateReceived(object);
                        } else if (type.equalsIgnoreCase("peer_joined")) {
                            callback.onPeerJoined();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "Connection closed: " + reason);
                    if (remote || code != 1000) {
                         callback.onError("Connection closed: " + reason);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "Error: " + ex.getMessage());
                    callback.onError("Connection Error: " + ex.getMessage());
                }
            };
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            callback.onError("Invalid Server URL");
        }
    }

    public void sendOffer(JSONObject offer) {
        if (socket != null && socket.isOpen()) {
            socket.send(offer.toString());
        }
    }

    public void sendAnswer(JSONObject answer) {
        if (socket != null && socket.isOpen()) {
            socket.send(answer.toString());
        }
    }

    public void sendIceCandidate(JSONObject candidate) {
        if (socket != null && socket.isOpen()) {
            socket.send(candidate.toString());
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.close();
        }
    }
}
