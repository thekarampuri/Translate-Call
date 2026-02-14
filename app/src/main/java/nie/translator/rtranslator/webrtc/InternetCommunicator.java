package nie.translator.rtranslator.webrtc;

import android.content.Context;
import org.json.JSONObject;

public class InternetCommunicator {
    private WebRTCClient webRTCClient;
    private SignalingClient signalingClient;
    private final Context context;
    private final String serverUrl;

    public interface Callback {
        void onConnected();
        void onDisconnected();
        void onPeerJoined();
        void onError(String message);
    }
    
    private Callback callback;

    public InternetCommunicator(Context context, String serverUrl) {
        this.context = context;
        this.serverUrl = serverUrl;
    }
    
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void connectToRoom(String roomId) {
        signalingClient = new SignalingClient(serverUrl, roomId, new SignalingClient.SignalingInterface() {
            @Override
            public void onOfferReceived(JSONObject data) {
                if (webRTCClient != null) {
                    webRTCClient.handleOffer(data);
                }
            }

            @Override
            public void onAnswerReceived(JSONObject data) {
                if (webRTCClient != null) {
                    webRTCClient.handleAnswer(data);
                }
            }

            @Override
            public void onIceCandidateReceived(JSONObject data) {
                if (webRTCClient != null) {
                    webRTCClient.handleIceCandidate(data);
                }
            }
            
            @Override
            public void onPeerJoined() {
                // Peer joined, we can start the call (as the initiator)
                if (callback != null) {
                    callback.onPeerJoined();
                }
                startCall();
            }

            @Override
            public void onError(String message) {
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
        
        webRTCClient = new WebRTCClient(context, signalingClient);
        signalingClient.connect();
        if (callback != null) {
            callback.onConnected();
        }
    }
    
    public void startCall() {
        if (webRTCClient != null) {
            webRTCClient.startCall();
        }
    }

    public void disconnect() {
        if (webRTCClient != null) {
            webRTCClient.close();
            webRTCClient = null;
        }
        if (signalingClient != null) {
            signalingClient.disconnect();
            signalingClient = null;
        }
        if (callback != null) {
            callback.onDisconnected();
        }
    }
}
