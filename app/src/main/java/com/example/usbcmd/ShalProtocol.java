package com.example.usbcmd;

/**
 * SHAL-1000 (ACCESS LINK) 프로토콜 헬퍼.
 * 패킷: STX(0x02) | Length(1,전체길이) | Command(1) | Data | ETX(0x03)
 */
public final class ShalProtocol {
    public static final byte STX = 0x02;
    public static final byte ETX = 0x03;
    public static final int SET_RELAYCONTROL = 0x00;
    public static final int ERR_SUCCESS = 0x00;

    private ShalProtocol() {}

    public static byte[] build(int command, byte[] data) {
        int dataLen = (data == null) ? 0 : data.length;
        int total = 4 + dataLen;
        if (total > 255) throw new IllegalArgumentException("data too long");
        byte[] p = new byte[total];
        p[0] = STX; p[1] = (byte) total; p[2] = (byte) command;
        if (dataLen > 0) System.arraycopy(data, 0, p, 3, dataLen);
        p[total - 1] = ETX;
        return p;
    }

    /**
     * SET_RELAYCONTROL. Data=Control(4, Ascii9): UseRelay, OutputType, Time(2자리)
     * @param useRelay 0=Relay0+1, 1=Relay0, 2=Relay1
     * @param onOff    0=OFF, 1=ON
     * @param timeSec  0~99초 (0=계속 유지)
     */
    public static byte[] relayControl(int useRelay, int onOff, int timeSec) {
        byte[] d = new byte[4];
        d[0] = (byte) ('0' + useRelay);
        d[1] = (byte) ('0' + onOff);
        String t = String.format("%02d", timeSec % 100);
        d[2] = (byte) t.charAt(0);
        d[3] = (byte) t.charAt(1);
        return build(SET_RELAYCONTROL, d);
    }

    public static String toHex(byte[] b) {
        if (b == null) return "(null)";
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02X ", x));
        return sb.toString().trim();
    }
}
