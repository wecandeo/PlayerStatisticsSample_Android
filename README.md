# 재생통계 Sample Player

## **설정**

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

## VOD
### Player 구성 방법
- DRM
  - 발급된 videoId, videoKey, gId, scretKey, packageId 를 통해 Player 구성
- Non DRM
  - 발급된 videoKey 로 영상 상세정보 조회를 하여 videoUrl 값을 이용하여 Player 구성
- 공통
  - Player 구성 이후 VodStatistics 객체 생성
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

## VodStatistics class
-  영상 상세정보 조회
```
public void fetchVideoDetail(String videoKey)
```
-  영상 통계정보 조회
```
private void fetchVideoStatistics(String videoKey)
```
- 플레이어 로드 통계 전송
```
private void playerLoadStatistics()
```
- 재생 / 다시시작 / 일시정지 / 정지 / SEEK
  - state : 재생 상태 ("PLAY", "RETRY", "PAUSE", "STOP", "SEEK")
```
public void sendStatistics(String state)
```
