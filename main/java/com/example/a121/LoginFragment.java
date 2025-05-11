package com.example.a121;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class LoginFragment extends Fragment {

    private EditText etApiToken, etOrgId, etDeviceNumber;
    private Button btnLogin;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        // 初始化控件
        etApiToken = rootView.findViewById(R.id.et_api_token);
        etOrgId = rootView.findViewById(R.id.et_org_id);
        etDeviceNumber = rootView.findViewById(R.id.et_device_number);
        btnLogin = rootView.findViewById(R.id.btn_login);

        // 设置点击事件
        btnLogin.setOnClickListener(v -> handleLogin());

        return rootView;
    }

    private void handleLogin() {
        String apiToken = etApiToken.getText().toString().trim();
        String orgId = etOrgId.getText().toString().trim();
        String deviceNumber = etDeviceNumber.getText().toString().trim();

        if (!apiToken.isEmpty() && !orgId.isEmpty() && !deviceNumber.isEmpty()) {
            // 存储登录信息
            SharedPreferences prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            prefs.edit()
                    .putString("api_token", apiToken)
                    .putString("org_id", orgId)
                    .putString("device_number", deviceNumber)
                    .apply();

            // 跳转到主界面
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
            getActivity().finish();  // 关闭当前 Fragment 的 Activity
        } else {
            Toast.makeText(requireContext(), "请填写所有字段", Toast.LENGTH_SHORT).show();
        }
    }
}
