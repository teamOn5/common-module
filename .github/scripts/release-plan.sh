#!/usr/bin/env bash
# 모듈별 배포 계획을 계산한다.
#
# 사용법: release-plan.sh [bump] [modules]
#   bump    : auto(기본) | patch | minor | major
#   modules : 강제로 대상을 좁힐 모듈 이름 (쉼표 구분, 비우면 전체)
#
# 모듈별 규칙
#   - 태그: 루트 프로젝트는 v1.2.3 (기존 태그 유지), 하위 모듈은 <모듈명>-v1.2.3
#   - 마지막 태그 이후, 해당 모듈 디렉토리 또는 공통 빌드 파일을 건드린 커밋만 본다
#   - 커밋 메시지(Conventional Commits)로 버전 결정
#       feat!: / fix!: ... 또는 본문 "BREAKING CHANGE" → major
#       feat:                                          → minor
#       fix: / perf:                                   → patch
#       그 외 (docs, chore, refactor, test, ci ...)     → 배포 안 함
#
# 결과 (현재 디렉토리에 생성)
#   release-plan.txt   : 배포할 모듈 한 줄씩 "이름|새버전|새태그|이전태그|경로들"
#   gradle-args.txt    : 모든 모듈의 버전 지정 인자 (-Pversion.<모듈>=x.y.z) + 배포 태스크
set -euo pipefail

BUMP_INPUT="${1:-auto}"
FORCE_MODULES="${2:-}"

# 모든 모듈에 영향을 주는 공통 파일
SHARED_PATHS=(build.gradle settings.gradle gradle.properties gradle gradlew gradlew.bat)

ROOT_NAME=$(sed -nE "s/^[[:space:]]*rootProject\.name[[:space:]]*=[[:space:]]*['\"]([^'\"]+)['\"].*/\1/p" settings.gradle)
mapfile -t SUBMODULES < <(grep -E '^[[:space:]]*include' settings.gradle | grep -oE "['\"][^'\"]+['\"]" | tr -d "'\":")

MODULES=("$ROOT_NAME" "${SUBMODULES[@]}")

tag_prefix() { if [ "$1" = "$ROOT_NAME" ]; then echo "v"; else echo "$1-v"; fi; }
module_paths() { if [ "$1" = "$ROOT_NAME" ]; then echo "src"; else echo "$1"; fi; }
publish_task() { if [ "$1" = "$ROOT_NAME" ]; then echo ":publish"; else echo ":$1:publish"; fi; }

in_force_list() {
  [ -z "$FORCE_MODULES" ] && return 0
  local m
  IFS=',' read -ra list <<< "$FORCE_MODULES"
  for m in "${list[@]}"; do [ "$(echo "$m" | xargs)" = "$1" ] && return 0; done
  return 1
}

: > release-plan.txt
version_args=()
publish_tasks=()

for module in "${MODULES[@]}"; do
  prefix=$(tag_prefix "$module")
  read -ra paths <<< "$(module_paths "$module")"
  latest=$(git tag --list "${prefix}[0-9]*.[0-9]*.[0-9]*" --sort=-v:refname | head -n1)
  current="${latest#"$prefix"}"
  current="${current:-0.0.0}"
  if [ -n "$latest" ]; then range="${latest}..HEAD"; else range="HEAD"; fi

  bump=none
  if in_force_list "$module"; then
    if [ "$BUMP_INPUT" != "auto" ]; then
      bump="$BUMP_INPUT"
    else
      subjects=$(git log "$range" --format='%s' -- "${paths[@]}" "${SHARED_PATHS[@]}")
      bodies=$(git log "$range" --format='%b' -- "${paths[@]}" "${SHARED_PATHS[@]}")
      type='[a-zA-Z]+(\([^)]*\))?'
      if echo "$subjects" | grep -Eq "^${type}!:" || echo "$bodies" | grep -Eq '^BREAKING[ -]CHANGE:'; then
        bump=major
      elif echo "$subjects" | grep -Eq '^feat(\([^)]*\))?:'; then
        bump=minor
      elif echo "$subjects" | grep -Eq '^(fix|perf)(\([^)]*\))?:'; then
        bump=patch
      fi
    fi
  fi

  next="$current"
  if [ "$bump" != "none" ]; then
    IFS='.' read -r major minor patch <<< "$current"
    case "$bump" in
      major) major=$((major + 1)); minor=0; patch=0 ;;
      minor) minor=$((minor + 1)); patch=0 ;;
      patch) patch=$((patch + 1)) ;;
    esac
    next="${major}.${minor}.${patch}"
    echo "${module}|${next}|${prefix}${next}|${latest}|${paths[*]}" >> release-plan.txt
    publish_tasks+=("$(publish_task "$module")")
    echo "[배포] ${module}: ${latest:-없음} → ${prefix}${next} (${bump})"
  else
    echo "[유지] ${module}: ${latest:-없음} (변경 없음)"
    if [ -z "$latest" ]; then
      echo "  ⚠ ${module} 은 아직 한 번도 배포되지 않았습니다. 이 모듈에 의존하는 모듈의 POM 이 0.0.0 을 가리킬 수 있습니다."
    fi
  fi

  # 배포하지 않는 모듈도 현재 버전을 넘겨야 의존하는 모듈의 POM 에 올바른 버전이 기록된다
  version_args+=("-Pversion.${module}=${next}")
done

echo "${version_args[*]} ${publish_tasks[*]}" > gradle-args.txt

if [ ${#publish_tasks[@]} -eq 0 ]; then
  echo "배포 대상 모듈이 없습니다."
fi
