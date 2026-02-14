package nie.translator.rtranslator.webrtc;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;

public class WebRTCClient {
    private static final String TAG = "WebRTCClient";
    private final Context context;
    private final SignalingClient signalingClient;
    private PeerConnection peerConnection;
    private PeerConnectionFactory peerConnectionFactory;
    private AudioTrack localAudioTrack;
    private EglBase rootEglBase;

    public WebRTCClient(Context context, SignalingClient signalingClient) {
        this.context = context;
        this.signalingClient = signalingClient;
        initializePeerConnectionFactory();
    }

    private void initializePeerConnectionFactory() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        rootEglBase = EglBase.create();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext()))
                .createPeerConnectionFactory();
    }

    private void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                JSONObject candidate = new JSONObject();
                try {
                    candidate.put("type", "candidate");
                    candidate.put("label", iceCandidate.sdpMLineIndex);
                    candidate.put("id", iceCandidate.sdpMid);
                    candidate.put("candidate", iceCandidate.sdp);
                    signalingClient.sendIceCandidate(candidate);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {}

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: " + iceConnectionState);
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

            @Override
            public void onAddStream(MediaStream mediaStream) {
                // Deprecated in newer WebRTC, but keeping for compatibility if strictly needed. 
                // However, modern implementations use onAddTrack.
                // If this causes compilation error (Missing Override), remove this method.
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {}

            @Override
            public void onDataChannel(DataChannel dataChannel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
                 if (receiver.track() instanceof AudioTrack) {
                     AudioTrack track = (AudioTrack) receiver.track();
                     track.setEnabled(true);
                     Log.d(TAG, "onAddTrack: Audio track received and enabled");
                 }
            }
        });

        // Add Local Audio
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        
        AudioSource audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource);
        peerConnection.addTrack(localAudioTrack);
    }

    public void startCall() {
        createPeerConnection();
        Log.d(TAG, "startCall: Creating Offer...");
        peerConnection.createOffer(new LogSdpObserver("createOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                Log.d(TAG, "startCall: Offer created. Setting Local Description...");
                peerConnection.setLocalDescription(new LogSdpObserver("setLocalDescription(Offer)"), sessionDescription);
                JSONObject offer = new JSONObject();
                try {
                    offer.put("type", "offer");
                    offer.put("sdp", sessionDescription.description);
                    signalingClient.sendOffer(offer);
                    Log.d(TAG, "startCall: Offer Sent");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }

    public void handleOffer(JSONObject offerData) {
        createPeerConnection();
        try {
            String sdp = offerData.getString("sdp");
            Log.d(TAG, "handleOffer: Received SDP Offer");
            peerConnection.setRemoteDescription(new LogSdpObserver("setRemoteDescription(Offer)") {
                @Override
                public void onSetSuccess() {
                    super.onSetSuccess();
                    Log.d(TAG, "handleOffer: Remote Description set. Creating Answer...");
                    peerConnection.createAnswer(new LogSdpObserver("createAnswer") {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            super.onCreateSuccess(sessionDescription);
                            Log.d(TAG, "handleOffer: Answer created. Setting Local Description...");
                            peerConnection.setLocalDescription(new LogSdpObserver("setLocalDescription(Answer)"), sessionDescription);
                            JSONObject answer = new JSONObject();
                            try {
                                answer.put("type", "answer");
                                answer.put("sdp", sessionDescription.description);
                                signalingClient.sendAnswer(answer);
                                Log.d(TAG, "handleOffer: Answer Sent");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new MediaConstraints());
                }
            }, new SessionDescription(SessionDescription.Type.OFFER, sdp));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void handleAnswer(JSONObject answerData) {
        try {
            String sdp = answerData.getString("sdp");
            Log.d(TAG, "handleAnswer: Received SDP Answer");
            peerConnection.setRemoteDescription(new LogSdpObserver("setRemoteDescription(Answer)"), 
                    new SessionDescription(SessionDescription.Type.ANSWER, sdp));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void handleIceCandidate(JSONObject candidateData) {
        try {
            IceCandidate candidate = new IceCandidate(
                    candidateData.getString("id"),
                    candidateData.getInt("label"),
                    candidateData.getString("candidate")
            );
            Log.d(TAG, "handleIceCandidate: Adding ICE Candidate");
            peerConnection.addIceCandidate(candidate);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public void close() {
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }
        if (rootEglBase != null) {
            rootEglBase.release();
        }
    }

    private static class LogSdpObserver implements SdpObserver {
        private final String tag;

        public LogSdpObserver(String tag) {
            this.tag = tag;
        }
        
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d(TAG, tag + ": onCreateSuccess");
        }
        
        @Override
        public void onSetSuccess() {
            Log.d(TAG, tag + ": onSetSuccess");
        }
        
        @Override
        public void onCreateFailure(String s) {
            Log.e(TAG, tag + ": onCreateFailure: " + s);
        }
        
        @Override
        public void onSetFailure(String s) {
            Log.e(TAG, tag + ": onSetFailure: " + s);
        }
    }
}
