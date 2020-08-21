# 재생통계 Sample Player

## **설정**

### 사전에 조회할 값
- 업데이트 예정

### 1. 라이브러리 추가
    implementation 'com.google.android.exoplayer:exoplayer:r2.5.1'
    implementation 'com.google.android.exoplayer:exoplayer-core:r2.5.1'
    implementation 'com.google.android.exoplayer:exoplayer-ui:r2.5.1'
    implementation 'com.android.volley:volley:1.1.1'
    implementation 'com.google.code.gson:gson:2.8.6'
    implementation files('libs/WecandeoPlaySdk.jar')

### - libs 폴더에 WecandeoPlaySdk.jar 파일 추가 

### 2. AndroidManifest.xml 에 permissions 추가
```
<uses-permission android:name="android.permission.INTERNET" />
```

### 3. AndroidManifest.xml 에 networkSecurityConfig 설정
```
<application
...
android:networkSecurityConfig="@xml/network_security_config">
```
- res 폴더 안에 xml 폴더 추가 / xml 폴더에 network_security_config.xml 파일 추가
  - netowkr_security_config.xml 내용
  ```
    <?xml version="1.0" encoding="utf-8"?>
    <network-security-config>
        <!--Set application-wide security config using base-config tag.-->
        <base-config cleartextTrafficPermitted="true"/>
    </network-security-config>
  ```  

### 4. - LiveStatistics, VodStatistics, StatisticsUrlInfo, RequestSingleton Class 는 수정하지 않고 사용

## VOD
### Player 구성 방법
- DRM
  - 발급된 videoId, videoKey, gId, scretKey, packageId 를 통해 Player 구성
- Non DRM
  - 발급된 videoKey 로 영상 상세정보 조회를 하여 나온 videoUrl 값을 이용하여 Player 구성
- 공통
  - Player 구성 이후 VodStatistics 객체 생성
     ```
    vodStatistics = new VodStatistics(getApplicationContext());
    ```
  - onPlayerStateChange(boolean playWhenReady, int playbackState) 오버라이드
    - 영상이 처음 로드되고 재생준비가 완료되면 다음 정보 셋팅
```
vodStatistics.setWecandeoSdk(wecandeoSdk);
vodStatistics.setDuration(TimeUnit.MILLISECONDS.toSeconds(wecandeoSdk.getPlayer().getDuration()));
vodStatistics.fetchVideoDetail("videoKey");
``` 

### 통계 전송 설명
- [x] 재생준비 완료
  - [x] 영상 상세정보 조회
  - [x] 영상 통계정보 조회
  - [x] 플레이어 로드 통계 전송
- [x] 재생 시 통계 전송
- [x] 재생 중
  - [x] 재생 중 통계 전송 
    - 재생시간 60초 이하 구간 5초마다 통계 전송
    - 재생시간 60초 이후 구간 10초마다 통계 전송
  - [x] 재생 중 구간 통계 전송(0 ~ 100%)
- [x] 재생 완료 시
  - [x] 정지 통계 전송
- [x] 재생 정지 시
  - [x] 정지 통계 전송
- [x] 일시정지 시 통계 전송
- [x] seekbar 이동 시 통계 전송
  - seekbar 는 최초 재생 이후 실행하도록 설정해야 합니다.

## VodStatistics class
-  영상 상세정보 조회
   - 영상 상세정보를 조회하게 되면 VodStatistics Class 에서는
영상 통계정보 조회 이후, 플레이어 로드 통계를 전송하게 됩니다.
```
public void fetchVideoDetail(String videoKey)
```
- 재생 / 다시시작 / 일시정지 / 정지 / SEEK
  - state : 재생 상태 ("PLAY", "RETRY", "PAUSE", "STOP", "SEEK")
```
public void sendStatistics(String state)
```

## Live
### Player 구성 방법
- 발급된 videoKey 로 영상 상세정보 조회를 하여 나온 videoUrl 값을 이용하여 Player 구성
- Player 구성 이후 통계 연동을 위한 LiveStatistics 객체 생성
```
liveStatistics = new LiveStatistics(getApplicationContext(), "videoKey");
```

### 통계 전송 설명
- [x] 영상 상태정보 조회
- [x] 재생 시
  - [x] 영상 상세정보 조회
  - [x] 재생 시 통계 전송
- [x] 재생 중 통계 전송
  - 5초마다 통계 전송
- [x] 정지 시 통계 전송

### LiveStatistics Class
- 재생
  - 재생 시 영상 상세정보 조회 이후 재생 통계 전송
```
liveStatistics.sendStatistics("PLAY")
```
- 정지
```
liveStatistics.sendStatistics("STOP")
```