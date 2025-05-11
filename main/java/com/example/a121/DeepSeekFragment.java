package com.example.a121;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DeepSeekFragment extends Fragment {

    private EditText editTextPrompt;
    private Button btnAsk;
    private TextView textViewResponse;

    private SharedViewModel sharedViewModel;
    private String latestHeartRate = "--";
    private String latestBreathRate = "--";
    private String latestTemperature = "--";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("DeepSeek", "onCreateView 被调用");

        View rootView = inflater.inflate(R.layout.fragment_deepseek, container, false);

        // 初始化控件
        editTextPrompt = rootView.findViewById(R.id.input_text);
        btnAsk = rootView.findViewById(R.id.send_button);
        textViewResponse = rootView.findViewById(R.id.response_text);

        // 绑定 ViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // 观察实时数据
        sharedViewModel.getHeartRate().observe(getViewLifecycleOwner(), value -> {
            if (value != null) latestHeartRate = value;
        });
        sharedViewModel.getBreathRate().observe(getViewLifecycleOwner(), value -> {
            if (value != null) latestBreathRate = value;
        });
        sharedViewModel.getBodyTemp().observe(getViewLifecycleOwner(), value -> {
            if (value != null) latestTemperature = value;
        });

        // 设置按钮点击事件
        btnAsk.setOnClickListener(v -> {
            Log.d("DeepSeek", "按钮被点击！");

            String userQuestion = editTextPrompt.getText().toString().trim();
            if (userQuestion.isEmpty()) {
                Toast.makeText(getContext(), "请输入问题", Toast.LENGTH_SHORT).show();
                return;
            }

            // 构造健康数据上下文并提问
            String prompt = String.format(
                    "以下是用户的健康数据：\n" +
                            "心率：%s 次/分钟\n" +
                            "呼吸率：%s 次/分钟\n" +
                            "体表温度乘10(较低是正常的）：%s ℃\n" +
                            "请根据这些数据分析用户的健康状态，并回答问题：%s",
                    latestHeartRate, latestBreathRate, latestTemperature, userQuestion
            );

            askDeepSeek(prompt);
        });

        return rootView;
    }

    private void askDeepSeek(String prompt) {
        Log.d("DeepSeek", "正在调用 DeepSeek API...");

        new Thread(() -> {
            try {
                // 构造 JSON 请求体
                JSONObject json = new JSONObject();
                json.put("model", "deepseek-chat");

                JSONArray messages = new JSONArray();

                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", "你是专业健康顾问，请根据提供的健康数据，用简洁语言给出建议。");
                messages.put(systemMsg);

                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                messages.put(userMsg);

                json.put("messages", messages);

                String apiKey = getString(R.string.deepseek_api_key);
                URL url = new URL("https://api.deepseek.com/v1/chat/completions");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream(),
                        "utf-8"
                ));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    responseBuilder.append(line);
                }

                JSONObject responseJson = new JSONObject(responseBuilder.toString());
                String content = responseJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                requireActivity().runOnUiThread(() -> textViewResponse.setText(content));

            } catch (Exception e) {
                Log.e("DeepSeek", "请求失败", e);
                requireActivity().runOnUiThread(() -> textViewResponse.setText("请求失败: " + e.getMessage()));
            }
        }).start();
    }
}
