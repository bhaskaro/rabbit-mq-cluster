#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

JAVA_HOME_DEFAULT="/scratch/voggu/softwares/jdk-25.0.1"
JAVA_HOME="${JAVA_HOME:-$JAVA_HOME_DEFAULT}"
PATH="$JAVA_HOME/bin:$PATH"
export JAVA_HOME PATH

APP_NAME="rabbitmq-cluster"
RUN_DIR="$PROJECT_ROOT/.run"
PID_FILE="$RUN_DIR/${APP_NAME}.pid"
LOG_FILE="$RUN_DIR/${APP_NAME}.log"
MVN_BIN="${MVN_BIN:-mvn}"
MVN_BIN="$(command -v "$MVN_BIN")"

mkdir -p "$RUN_DIR"

usage() {
  cat <<EOF
Usage: $(basename "$0") <start|stop|restart|status|logs>
EOF
}

get_pid() {
  if [[ -f "$PID_FILE" ]]; then
    cat "$PID_FILE"
  fi
}

is_running() {
  local pid
  pid="$(get_pid)"
  [[ -n "${pid:-}" ]] && kill -0 "$pid" 2>/dev/null
}

find_jar() {
  find "$PROJECT_ROOT/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.jar.original' | head -n 1
}

build_jar() {
  (
    cd "$PROJECT_ROOT"
    "$MVN_BIN" -DskipTests package >/dev/null
  )
}

start_app() {
  if is_running; then
    echo "$APP_NAME is already running with PID $(get_pid)"
    return 0
  fi

  rm -f "$PID_FILE" "$LOG_FILE"
  build_jar

  local jar_path
  jar_path="$(find_jar)"
  if [[ -z "${jar_path:-}" ]]; then
    echo "Unable to find runnable jar under $PROJECT_ROOT/target"
    return 1
  fi

  (
    cd "$PROJECT_ROOT"
    nohup "$JAVA_HOME/bin/java" -jar "$jar_path" >>"$LOG_FILE" 2>&1 < /dev/null &
    echo $! >"$PID_FILE"
  )

  sleep 5

  if is_running; then
    echo "$APP_NAME started with PID $(get_pid)"
    echo "Jar: $jar_path"
    echo "Log file: $LOG_FILE"
    return 0
  fi

  echo "Failed to start $APP_NAME. Check $LOG_FILE"
  rm -f "$PID_FILE"
  return 1
}

stop_app() {
  if ! is_running; then
    echo "$APP_NAME is not running"
    rm -f "$PID_FILE"
    return 0
  fi

  local pid
  pid="$(get_pid)"
  kill "$pid"

  for _ in $(seq 1 20); do
    if ! kill -0 "$pid" 2>/dev/null; then
      rm -f "$PID_FILE"
      echo "$APP_NAME stopped"
      return 0
    fi
    sleep 1
  done

  echo "Process $pid did not stop gracefully; sending SIGKILL"
  kill -9 "$pid" 2>/dev/null || true
  rm -f "$PID_FILE"
  echo "$APP_NAME stopped"
}

status_app() {
  if is_running; then
    echo "$APP_NAME is running with PID $(get_pid)"
  else
    echo "$APP_NAME is not running"
    rm -f "$PID_FILE"
  fi
}

logs_app() {
  if [[ ! -f "$LOG_FILE" ]]; then
    echo "No log file found at $LOG_FILE"
    return 1
  fi

  tail -n 50 "$LOG_FILE"
}

case "${1:-}" in
  start)
    start_app
    ;;
  stop)
    stop_app
    ;;
  restart)
    stop_app
    start_app
    ;;
  status)
    status_app
    ;;
  logs)
    logs_app
    ;;
  *)
    usage
    exit 1
    ;;
esac
