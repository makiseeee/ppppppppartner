package com.example.a121;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etApiToken, etOrgId, etDeviceNumber;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_login);


        etApiToken = findViewById(R.id.et_api_token);
        etOrgId = findViewById(R.id.et_org_id);
        etDeviceNumber = findViewById(R.id.et_device_number);
        btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> {
            String apiToken = etApiToken.getText().toString().trim();
            String orgId = etOrgId.getText().toString().trim();
            String deviceNumber = etDeviceNumber.getText().toString().trim();

            if (!apiToken.isEmpty() && !orgId.isEmpty() && !deviceNumber.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                prefs.edit()
                        .putString("api_token", apiToken)
                        .putString("org_id", orgId)
                        .putString("device_number", deviceNumber)
                        .apply();

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
            }
        });
    }
}