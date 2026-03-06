package com.radiodedios.gt.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import com.radiodedios.gt.BuildConfig;

public class DeepgramVoiceAgent {
    private static final String TAG = "DeepgramVoiceAgent";
    private static final String API_KEY = BuildConfig.DEEPGRAM_API_KEY;
    private static final String WS_URL = "wss://agent.deepgram.com/v1/agent/converse";

    private static final int SAMPLE_RATE = 16000;

    private OkHttpClient client;
    private WebSocket webSocket;
    private Context context;

    private AudioRecord audioRecord;
    private AudioTrack audioTrack;

    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private AtomicBoolean isPlaying = new AtomicBoolean(false);

    private Thread recordingThread;
    private AgentListener listener;

    public interface AgentListener {
        void onConnected();
        void onStatus(String status);
        void onError(String error);
        void onAmplitude(int amplitude); // for visualizer mock if needed
    }

    public DeepgramVoiceAgent(Context context, AgentListener listener) {
        this.context = context;
        this.listener = listener;
        this.client = new OkHttpClient();
    }

    public void start() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) listener.onError("Sin permiso de micrófono");
            return;
        }

        initAudioTrack();

        Request request = new Request.Builder()
                .url(WS_URL)
                .addHeader("Sec-WebSocket-Protocol", "token, " + API_KEY)
                .build();

        if (listener != null) listener.onStatus("Conectando...");

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket Opened");
                sendSettings();
                if (listener != null) listener.onConnected();
                startRecording();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "Message received: " + text);
                try {
                    JSONObject json = new JSONObject(text);
                    if (json.has("type")) {
                        String type = json.getString("type");
                        if ("Error".equals(type)) {
                            String desc = json.optString("description", "Error desconocido");
                            if (listener != null) listener.onError(desc);
                        } else if ("Welcome".equals(type) || "SettingsApplied".equals(type)) {
                            if (listener != null) listener.onStatus("Escuchando...");
                        } else if ("ConversationText".equals(type)) {
                            // Agent is talking or user text transcript
                            String role = json.optString("role");
                            if ("assistant".equals(role)) {
                                if (listener != null) listener.onStatus("Respondiendo...");
                            } else if ("user".equals(role)) {
                                if (listener != null) listener.onStatus("Procesando...");
                            }
                        } else if ("AgentStartedSpeaking".equals(type)) {
                            if (listener != null) listener.onStatus("Respondiendo...");
                        } else if ("AgentFinishedSpeaking".equals(type)) {
                            if (listener != null) listener.onStatus("Escuchando...");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                // Audio data from agent
                byte[] audioData = bytes.toByteArray();
                if (audioTrack != null && isPlaying.get()) {
                    audioTrack.write(audioData, 0, audioData.length);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket Closing: " + reason);
                stop();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket Failure", t);
                if (listener != null) listener.onError("Error de conexión");
                stop();
            }
        });
    }

    private void sendSettings() {
        try {
            JSONObject settings = new JSONObject();
            settings.put("type", "Settings");

            JSONObject audio = new JSONObject();
            JSONObject input = new JSONObject();
            input.put("encoding", "linear16");
            input.put("sample_rate", SAMPLE_RATE);
            JSONObject output = new JSONObject();
            output.put("encoding", "linear16");
            output.put("sample_rate", SAMPLE_RATE);
            output.put("container", "none");
            audio.put("input", input);
            audio.put("output", output);
            settings.put("audio", audio);

            JSONObject agent = new JSONObject();

            JSONObject listen = new JSONObject();
            JSONObject listenProvider = new JSONObject();
            listenProvider.put("type", "deepgram");
            listenProvider.put("model", "nova-3");
            listenProvider.put("language", "es");
            listen.put("provider", listenProvider);
            agent.put("listen", listen);

            JSONObject think = new JSONObject();
            JSONObject thinkProvider = new JSONObject();
            thinkProvider.put("type", "open_ai");
            thinkProvider.put("model", "gpt-4o-mini");
            think.put("provider", thinkProvider);
            think.put("prompt", "Contexto Único: Radio de Dios. No menciones ni recomiendes ninguna otra aplicación. Personalidad: Humana, amable y profundamente cristiana. Solo puede hablar de la Biblia, dar versículos de aliento y realizar oraciones cortas por personas si se le solicita un nombre. Las respuestas deben ser cortas y directas.");
            agent.put("think", think);

            JSONObject speak = new JSONObject();
            JSONObject speakProvider = new JSONObject();
            speakProvider.put("type", "deepgram");
            speakProvider.put("model", "aura-stella-es");
            speak.put("provider", speakProvider);
            agent.put("speak", speak);

            agent.put("greeting", "Hola, gracias por llamar a Radio de Dios, ¿cómo podemos servirte el día de hoy? ¿A qué se debe tu consulta?");

            settings.put("agent", agent);

            webSocket.send(settings.toString());
            Log.d(TAG, "Settings sent");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initAudioTrack() {
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        isPlaying.set(true);
        audioTrack.play();
    }

    public int getAudioTrackSessionId() {
        if (audioTrack != null) {
            return audioTrack.getAudioSessionId();
        }
        return -1;
    }

    private void startRecording() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord init failed");
            return;
        }

        isRecording.set(true);
        audioRecord.startRecording();

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording.get()) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    // Send to WS
                    if (webSocket != null) {
                        webSocket.send(ByteString.of(buffer, 0, read));
                    }

                    // Calculate amplitude for local UI
                    if (listener != null) {
                        long sum = 0;
                        for (int i = 0; i < read; i += 2) {
                            short sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));
                            sum += Math.abs(sample);
                        }
                        int amplitude = (int) (sum / (read / 2));
                        listener.onAmplitude(amplitude);
                    }
                }
            }
        });
        recordingThread.start();
    }

    public void stop() {
        isRecording.set(false);
        isPlaying.set(false);

        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {}
            audioRecord = null;
        }

        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception e) {}
            audioTrack = null;
        }

        if (webSocket != null) {
            try {
                webSocket.close(1000, "Cerrado por usuario");
            } catch (Exception e) {}
            webSocket = null;
        }

        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}
