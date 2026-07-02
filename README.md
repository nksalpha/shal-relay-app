# SHAL-1000 릴레이 제어 앱 (릴레이 0 / 릴레이 1 토글)

SHAL-1000(ACCESS LINK)을 USB-C로 안드로이드에 연결해 릴레이 2개를 버튼으로 제어합니다.
버튼을 누를 때마다 해당 릴레이가 ON↔OFF로 토글됩니다. (내부적으로 CH340 USB-Serial 통신)

## GitHub Actions로 APK 자동 빌드 (Android Studio 불필요)

1. GitHub에서 새 저장소(repository)를 만듭니다. (예: `shal-relay-app`)
2. 이 폴더의 **모든 파일**을 저장소에 올립니다.
   - 웹에서: 저장소 페이지 → "Add file" → "Upload files" → 폴더 내용 드래그 → Commit
   - 또는 git으로:
     ```bash
     git init
     git add .
     git commit -m "SHAL-1000 relay app"
     git branch -M main
     git remote add origin https://github.com/<사용자명>/shal-relay-app.git
     git push -u origin main
     ```
3. 업로드(push)되면 자동으로 빌드가 시작됩니다.
   저장소의 **Actions** 탭에서 "Build APK" 실행을 확인하세요. (약 3~5분)
   - 수동 실행: Actions 탭 → "Build APK" → "Run workflow"
4. 빌드가 끝나면 해당 실행 화면 하단 **Artifacts**의 `shal-relay-debug-apk`를 클릭해 다운로드합니다.
   압축을 풀면 `app-debug.apk`가 나옵니다.

## 폰에 설치
- APK를 폰으로 옮긴 뒤 실행 → "출처를 알 수 없는 앱 설치 허용"을 켜고 설치합니다.
  (디버그 서명된 APK라 그대로 설치·실행됩니다. 스토어 배포는 별도 릴리스 서명 필요)

## 사용법
1. SHAL-1000의 **USB-C 포트**를 폰에 연결합니다. (필요시 USB-C to C 케이블)
2. 앱 실행 → USB 권한 팝업 허용 → 상태가 "연결됨"으로 바뀝니다.
3. **릴레이 0 / 릴레이 1** 버튼을 누르면 토글됩니다. (초록=ON, 회색=OFF)

## ⚠️ 통신 속도(Baud Rate) 확인
문서에 USB 명령 채널의 baud rate가 명시되어 있지 않아 **9600**으로 설정했습니다.
연결은 되는데 릴레이가 동작하지 않으면 `MainActivity.java`의
`BAUD_RATE = 9600` 값을 115200 등으로 바꿔 다시 빌드하세요. (제조사: 신화시스템 070-7098-3934)

## 프로토콜 요약 (참고)
- 패킷: `STX(0x02) | Length | Command(0x00=SET_RELAYCONTROL) | Data(4) | ETX(0x03)`
- Data(ASCII): UseRelay(1=Relay0, 2=Relay1) · OutputType(0=OFF,1=ON) · Time(2자리 초, 00=계속)
- 예) 릴레이0 ON → `02 08 00 31 31 30 30 03` / 릴레이0 OFF → `02 08 00 31 30 30 30 03`
  (앱이 보내는 실제 바이트, 검증 완료)

## 빌드 구성 (참고)
AGP 8.5.2 · Gradle 8.7 · compileSdk 34 · minSdk 21 · JDK 17 · usb-serial-for-android 3.8.1
