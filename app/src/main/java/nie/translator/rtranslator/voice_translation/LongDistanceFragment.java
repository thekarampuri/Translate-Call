package nie.translator.rtranslator.voice_translation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import nie.translator.rtranslator.R;
import nie.translator.rtranslator.webrtc.InternetCommunicator;

public class LongDistanceFragment extends Fragment {
    private Button buttonCreateRoom, buttonJoinRoom;
    private EditText inputRoomId;
    private Button buttonConnect;
    private TextView textStatus;
    private TextView textRoomIdDisplay;
    private InternetCommunicator internetCommunicator;
    private boolean isConnected = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_long_distance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buttonCreateRoom = view.findViewById(R.id.buttonCreateRoom);
        buttonJoinRoom = view.findViewById(R.id.buttonJoinRoom);
        inputRoomId = view.findViewById(R.id.inputRoomId);
        buttonConnect = view.findViewById(R.id.buttonConnect);
        textStatus = view.findViewById(R.id.textStatus);
        textRoomIdDisplay = view.findViewById(R.id.textRoomIdDisplay);
        View buttonBack = view.findViewById(R.id.buttonBack);

        // Initialize InternetCommunicator
        // NOTE: Replace with your actual server URL
        String signalingServerUrl = "ws://192.168.0.102:8080"; 
        internetCommunicator = new InternetCommunicator(requireContext(), signalingServerUrl);
        setupCommunicatorCallback();

        // Developer Option: Long click status to change IP
        textStatus.setOnClickListener(v -> {
            showServerIpDialog();
        });

        // Mode: Create Room
        buttonCreateRoom.setOnClickListener(v -> {
            String roomId = generateRoomId();
            textRoomIdDisplay.setText("Room ID: " + roomId);
            internetCommunicator.connectToRoom(roomId);
            showActiveUI();
            isConnected = true;
        });

        // Mode: Join Room
        buttonJoinRoom.setOnClickListener(v -> {
            showJoinUI();
        });

        // Connect Button (for Join Mode)
        buttonConnect.setOnClickListener(v -> {
            String roomId = inputRoomId.getText().toString();
            if (!roomId.isEmpty()) {
                textRoomIdDisplay.setText("Room ID: " + roomId);
                internetCommunicator.connectToRoom(roomId);
                showActiveUI();
                isConnected = true;
            } else {
                Toast.makeText(getContext(), "Enter a Room ID", Toast.LENGTH_SHORT).show();
            }
        });

        buttonBack.setOnClickListener(v -> {
            if (isConnected) {
                internetCommunicator.disconnect();
                resetUI();
            } else if (inputRoomId.getVisibility() == View.VISIBLE) {
                resetUI(); // Back from Join input to Selection
            } else {
                if (getActivity() instanceof VoiceTranslationActivity) {
                    ((VoiceTranslationActivity) getActivity()).setFragment(VoiceTranslationActivity.TRANSLATION_FRAGMENT);
                }
            }
        });
    }

    private void showJoinUI() {
        buttonCreateRoom.setVisibility(View.GONE);
        buttonJoinRoom.setVisibility(View.GONE);
        inputRoomId.setVisibility(View.VISIBLE);
        buttonConnect.setVisibility(View.VISIBLE);
        textStatus.setVisibility(View.GONE);
        textRoomIdDisplay.setVisibility(View.GONE);
        buttonConnect.setText("Connect");
    }

    private void showActiveUI() {
        buttonCreateRoom.setVisibility(View.GONE);
        buttonJoinRoom.setVisibility(View.GONE);
        inputRoomId.setVisibility(View.GONE);
        buttonConnect.setVisibility(View.GONE);
        textStatus.setVisibility(View.VISIBLE);
        textRoomIdDisplay.setVisibility(View.VISIBLE);
        textStatus.setText("Status: Waiting for Peer...");
    }

    private void resetUI() {
        isConnected = false;
        buttonCreateRoom.setVisibility(View.VISIBLE);
        buttonJoinRoom.setVisibility(View.VISIBLE);
        inputRoomId.setVisibility(View.GONE);
        buttonConnect.setVisibility(View.GONE);
        textStatus.setVisibility(View.GONE);
        textRoomIdDisplay.setVisibility(View.GONE);
        inputRoomId.setText("");
    }

    private String generateRoomId() {
        int randomId = (int) (Math.random() * 900000) + 100000; // 100000 to 999999
        return String.valueOf(randomId);
    }
    
    private void showServerIpDialog() {
        final EditText input = new EditText(getContext());
        input.setHint("192.168.0.102:8080");
        input.setText("192.168.0.102:8080"); // Default

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Set Server IP")
                .setMessage("Enter the IP and Port of your Signaling Server (e.g., 192.168.0.102:8080)")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String ip = input.getText().toString();
                    if (!ip.isEmpty()) {
                        String newUrl = "ws://" + ip;
                        internetCommunicator = new InternetCommunicator(requireContext(), newUrl);
                        setupCommunicatorCallback(); // Re-attach callback
                        Toast.makeText(getContext(), "Server IP Updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void setupCommunicatorCallback() {
        internetCommunicator.setCallback(new InternetCommunicator.Callback() {
            @Override
            public void onConnected() {
                requireActivity().runOnUiThread(() -> textStatus.setText("Status: Waiting for Peer..."));
            }

            @Override
            public void onDisconnected() {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                    resetUI();
                });
            }

            @Override
            public void onPeerJoined() {
                requireActivity().runOnUiThread(() -> textStatus.setText("Status: Peer Joined - Call Started"));
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    resetUI();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (internetCommunicator != null) {
            internetCommunicator.disconnect();
        }
    }
}
