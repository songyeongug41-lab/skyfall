# Gus Boss (Forge 1.20.1)

Gus, The Iron Golem - 커스텀 보스 모드. 현재 1단계(본체)까지 들어 있습니다.
모드 ID: `gusboss` / Forge 47.2.0 / Java 17

## GitHub에서 빌드하기
1. GitHub에서 새 저장소(Repository)를 만듭니다. (Private도 가능)
2. 이 폴더의 파일 전체를 저장소 맨 위(루트)에 올립니다.
   `.github` 폴더(숨김 폴더)와 `.gitignore`도 함께 올라가야 합니다.
   git 명령어로 올리는 경우:
       git init
       git add .
       git commit -m "init"
       git branch -M main
       git remote add origin <저장소 주소>
       git push -u origin main
3. 저장소의 Actions 탭에서 "Build mod"가 자동으로 실행됩니다. (처음에는 5~10분 걸립니다)
   자동으로 안 돌면 Actions 탭 > Build mod > Run workflow 를 누르세요.
4. 성공하면 실행 화면 맨 아래 Artifacts 의 `gusboss-jar` 를 내려받아 압축을 풀면 jar가 나옵니다.
   그 jar를 마인크래프트 mods 폴더에 넣으면 됩니다.
5. 실패하면 로그(빨간 단계를 눌러 나오는 내용)를 복사해서 보내주세요.

코드를 고치고 다시 올리면(push) 자동으로 새로 빌드됩니다.

## 들어있는 것
- 엔티티 `gusboss:gus` (철 골렘 크기): 체력 500, 방어력 15, 이동 속도 0.40, 공격력 12(자리표시자)
- 플레이어를 추적하며 0.5초 간격으로 좌우 주먹을 번갈아 공격 (공격 1)
- 전투 중 자가 회복 (초당 2, 자리표시자), 기본 보스바(빨간색)
- 확정 드롭: 네더라이트 주괴 20개, 불사의 토템 1개
- 스폰 알 (크리에이티브 > 스폰 알 탭), 소환 명령어: /summon gusboss:gus
- 외형은 바닐라 철 골렘 텍스처를 임시 사용 (GusRenderer 의 TEXTURE 만 교체하면 됨)

## 수치 조정
src/main/java/com/example/gusboss/GusStats.java 한 곳에 모여 있습니다.

## 테스트 순서
1. 게임이 모드 로드 에러 없이 켜지는지
2. 스폰 알로 소환 -> 모양과 걷기 애니메이션
3. 플레이어 추적, 좌우 주먹 번갈아 공격
4. 보스바 표시, 전투 중 체력 회복
5. 처치 시 네더라이트 주괴 20개 + 불사의 토템 드롭

## 다음 단계 (예정)
공격 2 (공중 대시, 지면 강타) -> 등반, 선택적 블록 파괴 -> 2페이즈 전환 -> 공격 3~5 -> 커스텀 보스바
