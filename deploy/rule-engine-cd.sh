#!/bin/bash
# 에러 발생 시 스크립트 실행을 즉시 중단
set -e

cd ~/rule-engine

set -a
source ~/infra/common.env
set +a
NEW_TAG="${IMAGE_TAG:-latest}"
LAST_GOOD_FILE=".last-good-tag"
OLD_TAG=$(cat "$LAST_GOOD_FILE" 2>/dev/null || echo "latest")

SERVICES=("rule-engine-1" "rule-engine-2")
PORTS=("10406" "10407")

# 지정한 태그로 전체 인스턴스 롤링 배포, 실패하면 false 반환
deploy_tag() {
  local tag="$1"
  export IMAGE_TAG="$tag"
  docker compose pull

  for i in "${!SERVICES[@]}"; do
    SERVICE="${SERVICES[$i]}"
    PORT="${PORTS[$i]}"

    # 유레카 및 라우터에서 해당 인스턴스 제외
    echo "${SERVICE} 유레카 상태 변경 (OUT_OF_SERVICE)"
    curl -sf -X POST -H "Content-Type: application/json" \
      -d '{"status": "OUT_OF_SERVICE"}' \
      "http://127.0.0.1:${PORT}/actuator/serviceregistry"

    echo "${SERVICE} 유레카 갱신 대기 (65초)"
    sleep 65;

    echo "${SERVICE} 재배포 (tag=${tag})"
    docker compose up -d --force-recreate "$SERVICE"

    for attempt in {1..30}; do
      if curl -sf "http://127.0.0.1:${PORT}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
        echo "${SERVICE} 배포 성공"
        break;
      fi

      if [ "$attempt" -eq 30 ]; then
        echo "${SERVICE} 배포 실패 (타임아웃, tag=${tag})"
        return 1;
      fi

      sleep 2;
    done
  done

  return 0
}

if deploy_tag "$NEW_TAG"; then
  echo "$NEW_TAG" > "$LAST_GOOD_FILE"
  docker image prune -f
else
  echo "새 버전(${NEW_TAG}) 배포 실패, 이전 버전(${OLD_TAG})으로 롤백"
  deploy_tag "$OLD_TAG" || echo "롤백도 실패함, 수동 확인 필요"
  docker image prune -f
  exit 1
fi
