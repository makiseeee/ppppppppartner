package com.example.a121;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import java.util.UUID;
import java.nio.charset.StandardCharsets;



public class SendFragment extends Fragment {

    private WebSocket webSocket;
    private EditText editTemperature;
    private Button btnPowerOn;
    private Button btnPowerOff;
    private Button btnAuto;
    private Button btnCooling;
    private Button btnHeating;
    private Button btnSendCommand;

    private String apiToken;
    private String orgId;
    private String deviceNumber;

    private OkHttpClient client;

    private static final String TAG = "SendFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send, container, false);

        // 绑定控件
        editTemperature = view.findViewById(R.id.editTemperature);
        btnPowerOn = view.findViewById(R.id.btnPowerOn);
        btnPowerOff = view.findViewById(R.id.btnPowerOff);
        btnAuto = view.findViewById(R.id.btnAuto);
        btnCooling = view.findViewById(R.id.btnCooling);
        btnHeating = view.findViewById(R.id.btnHeating);
        btnSendCommand = view.findViewById(R.id.btnSendCommand);

        // 初始化参数
        apiToken = getString(R.string.api_token);
        orgId = getString(R.string.org_id);
        deviceNumber = getString(R.string.device_number);

        client = new OkHttpClient();

        // 连接WebSocket
        connectWebSocket();

        // 开机按钮点击事件
        btnPowerOn.setOnClickListener(v -> {
            enableControls(true); // 启用控件
            sendCommand("A 02 24 F");
        });

        // 关机按钮点击事件
        btnPowerOff.setOnClickListener(v -> {
            enableControls(false); // 禁用控件
            sendCommand("A 03 24 F");
        });

        // 自动按钮点击事件
        btnAuto.setOnClickListener(v -> sendCommand("A 01 24 F"));

        // 制冷按钮点击事件
        btnCooling.setOnClickListener(v -> sendCommand("A 04 24 F"));

        // 制热按钮点击事件
        btnHeating.setOnClickListener(v -> sendCommand("A 05 24 F"));

        // 发送按钮点击事件
        btnSendCommand.setOnClickListener(v -> {
            String temperature = editTemperature.getText().toString().trim();
            if (validateTemperature(temperature)) {
                sendCommand("A 04 " + temperature + " F");
            }
        });

        return view;
    }

    private void connectWebSocket() {
        String wsUrl = String.format("wss://cloud.alientek.com/connection/%s/org/%s?token=%s",
                apiToken, orgId, UUID.randomUUID().toString().replace("-", ""));

        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                Log.d(TAG, "WebSocket连接成功");
                showToast("WebSocket已连接");
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                Log.e(TAG, "WebSocket连接失败: " + t.getMessage());
                showToast("WebSocket连接失败");
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "收到消息: " + text);
            }
        });
    }

    private void enableControls(boolean enable) {
        // 启用或禁用控件
        btnAuto.setEnabled(enable);
        btnCooling.setEnabled(enable);
        btnHeating.setEnabled(enable);
        editTemperature.setEnabled(enable);
        btnSendCommand.setEnabled(enable);
    }

    private boolean validateTemperature(String temperature) {
        try {
            int value = Integer.parseInt(temperature);
            if (value < 16 || value > 30) {
                showToast("温度必须在16到30摄氏度之间");
                return false;
            }
        } catch (NumberFormatException e) {
            showToast("请输入有效的温度数字");
            return false;
        }
        return true;
    }

    private void sendCommand(String message) {
        if (webSocket == null) {
            showToast("WebSocket未连接");
            return;
        }

        byte[] header = new byte[]{0x03};

        // 设备编号处理（20字节，ASCII编码，不足补0）
        byte[] deviceBytes = deviceNumber.getBytes(StandardCharsets.US_ASCII);
        byte[] fullDeviceBytes = new byte[20];
        System.arraycopy(deviceBytes, 0, fullDeviceBytes, 0, Math.min(deviceBytes.length, 20));

        // 消息内容（字符串编码成字节）
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        // 整体拼接：1字节头 + 20字节设备号 + 消息内容
        byte[] payload = new byte[1 + 20 + messageBytes.length];
        System.arraycopy(header, 0, payload, 0, 1);
        System.arraycopy(fullDeviceBytes, 0, payload, 1, 20);
        System.arraycopy(messageBytes, 0, payload, 21, messageBytes.length);

        // 发送
        webSocket.send(ByteString.of(payload));
        showToast("已发送: " + message);
    }


    private void showToast(String text) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webSocket != null) {
            webSocket.close(1000, "Fragment销毁");
        }
    }
}
