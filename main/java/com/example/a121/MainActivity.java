package com.example.a121;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.navigation.NavigationView;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import android.view.Menu;
import android.content.Intent;
import androidx.lifecycle.ViewModelProvider;  // 添加这行
import android.widget.Toast;
import java.io.IOException;
import android.os.Handler;
import android.os.Looper;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private TextView tvHeartRate, tvBreathRate, tvSleepStatus, tvRoomTemp, tvBodyTemp,tvPeopleExist,tvMoveLevel,tvPeopleDistance;
    private OkHttpClient client;
    private WebSocket webSocket;
    private String apiToken, orgId, deviceNumber;
    private DrawerLayout drawer;
    private Fragment currentFragment;
    private SharedViewModel sharedViewModel;

    // 用于限制更新频率
    private static final int MESSAGE_UPDATE_INTERVAL = 1000; // 1秒
    private long lastUpdateTime = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateUIRunnable;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

        // 初始化 Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        apiToken = getString(R.string.api_token);
        orgId = getString(R.string.org_id);
        deviceNumber = getString(R.string.device_number);

        client = new OkHttpClient();

        // 确保初始化完成后再连接
        connectWebSocket();

        // 初始化侧边栏
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // 默认加载 HomeFragment，只在首次创建 Activity 时加载
        if (savedInstanceState == null) {
            currentFragment = new HomeFragment();  // 如果你用的是 HomeFragment 显示主界面
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, currentFragment)
                    .commit();
            navigationView.setCheckedItem(R.id.nav_home);  // 可选：默认选中首页
        }
    }

    private void connectWebSocket() {
        String randomToken = UUID.randomUUID().toString().replace("-", "");
        String wsUrl = String.format("wss://cloud.alientek.com/connection/%s/org/%s?token=%s",
                apiToken, orgId, randomToken);

        Log.d("WebSocket", "连接URL: " + wsUrl); // 添加连接日志

        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d("WebSocket", "连接成功");
                byte[] header = {0x01};
                byte[] deviceBytes = deviceNumber.getBytes(StandardCharsets.US_ASCII);
                byte[] subscribeMsg = new byte[header.length + deviceBytes.length];
                System.arraycopy(header, 0, subscribeMsg, 0, header.length);
                System.arraycopy(deviceBytes, 0, subscribeMsg, header.length, deviceBytes.length);
                webSocket.send(ByteString.of(subscribeMsg));
                Log.d("WebSocket", "已发送订阅消息");
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d("WebSocket", "收到二进制数据，长度: " + bytes.size());
                // 在主线程上处理消息
                handler.removeCallbacks(updateUIRunnable);
                updateUIRunnable = new Runnable() {
                    @Override
                    public void run() {
                        processBinaryMessage(bytes.toByteArray());
                    }
                };
                handler.postDelayed(updateUIRunnable, 500); // 每500ms处理一次
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("WebSocket", "连接失败：" + t.getClass().getName() + ": " + t.getMessage(), t);

                if (response != null) {
                    Log.e("WebSocket", "响应码: " + response.code());
                    try {
                        Log.e("WebSocket", "响应体: " + response.body().string());
                    } catch (IOException e) {
                        Log.e("WebSocket", "读取响应体失败", e);
                    }
                }

                runOnUiThread(() -> Toast.makeText(MainActivity.this, "WebSocket连接失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void processBinaryMessage(byte[] data) {
        Log.d("WebSocket", "处理原始数据，长度: " + data.length);
        if (data.length < 1) return;

        byte header = data[0];
        if (header == 0x04) {
            String deviceId = new String(data, 1, 20, StandardCharsets.US_ASCII).trim();
            Log.d("WebSocket", "设备上线: " + deviceId);
        } else if (header == 0x05) {
            String deviceId = new String(data, 1, 20, StandardCharsets.US_ASCII).trim();
            Log.d("WebSocket", "设备离线: " + deviceId);
        } else if (header == 0x06 && data.length >= 21) {
            String message = new String(data, 21, data.length - 21, StandardCharsets.UTF_8);
            Log.d("WebSocket", "收到有效数据: " + message);
            parseData(message);
        } else {
            Log.w("WebSocket", "未知消息类型或长度不足");
        }
    }

    private void parseData(String message) {
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            String prefix = parts[0];
            String value = parts[1];

            // 更新 ViewModel
            switch (prefix) {
                case "hr":
                    sharedViewModel.setHeartRate(value);
                    Log.d("parseData", "收到前缀: " + prefix + ", 值: " + value);
                    break;
                case "br":
                    sharedViewModel.setBreathRate(value);
                    Log.d("parseData", "收到前缀: " + prefix + ", 值: " + value);
                    break;
                case "st":
                    sharedViewModel.setSleepStatus(value);
                    Log.d("parseData", "收到前缀: " + prefix + ", 值: " + value);
                    break;
                case "rt":
                    sharedViewModel.setRoomTemp(value);
                    Log.d("parseData", "收到前缀: " + prefix + ", 值: " + value);
                    break;
                case "bt":
                    sharedViewModel.setBodyTemp(value);
                    Log.d("parseData", "收到前缀: " + prefix + ", 值: " + value);
                    break;
                case "ds":
                    sharedViewModel.setPeopleDistance(value);
                    Log.d("parseData", "收到前缀: " + prefix + ", 值: " + value);
                    break;
                case "sp":
                    sharedViewModel.setMoveLevel(value);
                    Log.d("parseData", "收到前缀: " + prefix + ", 值: " + value);
                    break;
            }

            // 保留原有数据库存储逻辑
            storeData(prefix, value);
        }
    }

    private void storeData(String prefix, String value) {
        SensorData data = new SensorData();
        data.timestamp = System.currentTimeMillis();
        data.dataType = prefix;
        data.value = value;
        new Thread(() -> MyApplication.getDatabase().sensorDataDao().insert(data)).start();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        Fragment fragment = null;

        if (itemId == R.id.nav_home) {
            if (!(currentFragment instanceof HomeFragment)) {
                fragment = new HomeFragment();
            }
        } else if (itemId == R.id.nav_sleep) {
            if (!(currentFragment instanceof SleepFragment)) {
                fragment = new SleepFragment();
            }
        } else if (itemId == R.id.nav_breath) {
            if (!(currentFragment instanceof BreathFragment)) {
                fragment = new BreathFragment();
            }
        } else if (itemId == R.id.nav_heart_rate) {
            if (!(currentFragment instanceof HeartRateFragment)) {
                fragment = new HeartRateFragment();
            }
        } else if (itemId == R.id.nav_temp) {
            if (!(currentFragment instanceof TempFragment)) {
                fragment = new TempFragment();
            }
        } else if (itemId == R.id.nav_deepseek) {
            if (!(currentFragment instanceof DeepSeekFragment)) {
                fragment = new DeepSeekFragment();
            }
        }
        else if (itemId == R.id.nav_send) {
            if (!(currentFragment instanceof SendFragment)) {
                fragment = new SendFragment();
            }
        }
        else if (itemId == R.id.nav_relogin) {
            if (!(currentFragment instanceof LoginFragment)) {
                fragment = new LoginFragment();
            }
        }

        if (fragment != null) {
            loadFragment(fragment);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            currentFragment = fragment;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (currentFragment instanceof LoginFragment) {
            finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();




        if (webSocket != null) {
            webSocket.close(1000, "App closed");
        }
    }
}
