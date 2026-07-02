package com.example.usbcmd;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.List;

/**
 * SHAL-1000 릴레이 제어 앱 (릴레이 0 / 릴레이 1 토글).
 * 장치는 USB-C 연결 시 CH340 USB-Serial로 통신 → usb-serial-for-android 사용.
 */
public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.example.usbcmd.USB_PERMISSION";

    // ⚠️ 통신 속도: 문서에 명시가 없어 9600으로 설정. 응답 없으면 115200 등으로 변경.
    private static final int BAUD_RATE = 57600;
    private static final int WRITE_TIMEOUT_MS = 2000;

    private UsbManager usbManager;
    private UsbSerialPort serialPort;

    private boolean relay0On = false, relay1On = false;

    private Button connectBtn, relay0Btn, relay1Btn;
    private TextView statusView, logView;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) connect();
                else log("권한 거부됨");
            }
        }
    };

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        connectBtn = findViewById(R.id.connectButton);
        relay0Btn = findViewById(R.id.relay0Button);
        relay1Btn = findViewById(R.id.relay1Button);
        statusView = findViewById(R.id.statusView);
        logView = findViewById(R.id.logView);
        logView.setMovementMethod(new ScrollingMovementMethod());

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        IntentFilter f = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(usbReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(usbReceiver, f);

        connectBtn.setOnClickListener(v -> connect());
        relay0Btn.setOnClickListener(v -> toggleRelay(0));
        relay1Btn.setOnClickListener(v -> toggleRelay(1));

        updateButtons();
        connect();
    }

    private void connect() {
        if (serialPort != null) { setStatus("이미 연결됨", true); return; }
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (drivers.isEmpty()) { setStatus("장치 없음 (USB-C 연결 확인)", false); return; }

        UsbSerialDriver driver = drivers.get(0);
        UsbDevice dev = driver.getDevice();
        if (!usbManager.hasPermission(dev)) {
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), flags);
            usbManager.requestPermission(dev, pi);
            setStatus("권한 요청 중...", false);
            return;
        }
        UsbDeviceConnection conn = usbManager.openDevice(dev);
        if (conn == null) { setStatus("openDevice 실패", false); return; }
        try {
            serialPort = driver.getPorts().get(0);
            serialPort.open(conn);
            serialPort.setParameters(BAUD_RATE, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            setStatus(String.format("연결됨 VID=0x%04X PID=0x%04X @%d",
                    dev.getVendorId(), dev.getProductId(), BAUD_RATE), true);
        } catch (Exception e) {
            setStatus("연결 실패: " + e.getMessage(), false);
            serialPort = null;
        }
    }

    /** 릴레이 토글. idx 0→UseRelay 1(Relay0), idx 1→UseRelay 2(Relay1). Time 0=계속유지 */
    private void toggleRelay(int idx) {
        if (serialPort == null) { log("먼저 '연결'을 누르세요."); return; }
        boolean newState = (idx == 0) ? !relay0On : !relay1On;
        int useRelay = (idx == 0) ? 1 : 2;
        byte[] pkt = ShalProtocol.relayControl(useRelay, newState ? 1 : 0, 0);
        try {
            serialPort.write(pkt, WRITE_TIMEOUT_MS);
            if (idx == 0) relay0On = newState; else relay1On = newState;
            updateButtons();
            log("릴레이 " + idx + " → " + (newState ? "ON" : "OFF") + "   TX: " + ShalProtocol.toHex(pkt));
        } catch (Exception e) {
            log("전송 실패: " + e.getMessage());
        }
    }

    private void updateButtons() {
        relay0Btn.setText(relay0On ? "릴레이 0 : ON  (누르면 OFF)" : "릴레이 0 : OFF  (누르면 ON)");
        relay1Btn.setText(relay1On ? "릴레이 1 : ON  (누르면 OFF)" : "릴레이 1 : OFF  (누르면 ON)");
        relay0Btn.setBackgroundColor(relay0On ? Color.parseColor("#2E7D32") : Color.parseColor("#B0BEC5"));
        relay1Btn.setBackgroundColor(relay1On ? Color.parseColor("#2E7D32") : Color.parseColor("#B0BEC5"));
    }

    private void setStatus(String msg, boolean ok) {
        runOnUiThread(() -> {
            statusView.setText(msg);
            statusView.setTextColor(ok ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
        });
        log(msg);
    }

    private void log(String msg) { runOnUiThread(() -> logView.append(msg + "\n")); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serialPort != null) try { serialPort.close(); } catch (Exception ignored) {}
        try { unregisterReceiver(usbReceiver); } catch (Exception ignored) {}
    }
}
