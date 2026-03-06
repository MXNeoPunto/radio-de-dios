package com.radiodedios.gt.ui;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.radiodedios.gt.R;
import com.radiodedios.gt.service.DeepgramVoiceAgent;

public class VoiceAgentDialog extends DialogFragment implements DeepgramVoiceAgent.AgentListener {

    private View voiceCircle;
    private TextView textStatus;
    private DeepgramVoiceAgent agent;
    private Handler mainHandler;
    private Visualizer visualizer;
    private float currentScale = 1.0f;
    private boolean isAgentSpeaking = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        return inflater.inflate(R.layout.dialog_voice_agent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        voiceCircle = view.findViewById(R.id.voiceCircle);
        textStatus = view.findViewById(R.id.textStatus);
        View btnClose = view.findViewById(R.id.btnClose);

        mainHandler = new Handler(Looper.getMainLooper());

        btnClose.setOnClickListener(v -> dismiss());

        agent = new DeepgramVoiceAgent(requireContext(), this);
        agent.start();
    }

    @Override
    public void onConnected() {
        mainHandler.post(() -> {
            textStatus.setText("Conectado");
            setupVisualizer();
        });
    }

    private void setupVisualizer() {
        int sessionId = agent.getAudioTrackSessionId();
        if (sessionId != -1) {
            try {
                visualizer = new Visualizer(sessionId);
                visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
                visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                    @Override
                    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                        if (!isAgentSpeaking) return;
                        int sum = 0;
                        for (byte b : waveform) {
                            sum += Math.abs(b);
                        }
                        int amplitude = sum / waveform.length;
                        animateCircle(amplitude, true);
                    }

                    @Override
                    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false);
                visualizer.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onStatus(String status) {
        mainHandler.post(() -> {
            textStatus.setText(status);
            if (status.equals("Respondiendo...")) {
                isAgentSpeaking = true;
            } else {
                isAgentSpeaking = false;
                if (!status.equals("Escuchando...")) {
                    animateCircle(0, false);
                }
            }
        });
    }

    @Override
    public void onError(String error) {
        mainHandler.post(() -> {
            textStatus.setText("Error: " + error);
            Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onAmplitude(int amplitude) {
        if (!isAgentSpeaking) {
            // Animate using microphone input if agent is not speaking
            mainHandler.post(() -> animateCircle(amplitude, false));
        }
    }

    private void animateCircle(int amplitude, boolean isVisualizer) {
        // Normalizing amplitude
        // Visualizer amplitude (waveform) is typically 0-128
        // Mic amplitude (16-bit PCM) can be 0-32767

        float targetScale = 1.0f;

        if (isVisualizer) {
            targetScale = 1.0f + (amplitude / 128f) * 0.5f;
        } else {
            targetScale = 1.0f + (Math.min(amplitude, 10000) / 10000f) * 0.5f;
        }

        // Use ObjectAnimator to pulse the circle according to the new scale target
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(voiceCircle, "scaleX", currentScale, targetScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(voiceCircle, "scaleY", currentScale, targetScale);

        scaleX.setDuration(100);
        scaleY.setDuration(100);

        scaleX.start();
        scaleY.start();

        currentScale = targetScale;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (visualizer != null) {
            visualizer.release();
        }
        if (agent != null) {
            agent.stop();
        }
    }
}
