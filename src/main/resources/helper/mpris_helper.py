#!/usr/bin/env python3
import base64
import http.server
import json
import os
import subprocess
import sys
import tempfile
import threading
import urllib.parse
import urllib.request

PORT = 38472

_cover_lock = threading.Lock()
_last_art_url = ""
_ready_cover_hash = None
_ready_cover_b64 = None


def _fetch_cover_worker(art_url, target_hash):
    global _ready_cover_hash, _ready_cover_b64
    raw = None
    try:
        if art_url.startswith("file://"):
            parsed = urllib.parse.urlparse(art_url)
            local_path = urllib.parse.unquote(parsed.path)
            if os.path.isfile(local_path):
                with open(local_path, "rb") as f:
                    raw = f.read()
        elif art_url.startswith("http://") or art_url.startswith("https://"):
            req = urllib.request.Request(art_url, headers={"User-Agent": "MusicSubtitles/2.7"})
            with urllib.request.urlopen(req, timeout=4.0) as resp:
                if resp.status == 200:
                    raw = resp.read()
    except Exception:
        raw = None

    if raw:
        encoded = base64.b64encode(raw).decode("ascii")
        with _cover_lock:
            if art_url == _last_art_url:
                _ready_cover_b64 = encoded
                _ready_cover_hash = target_hash


def get_mpris_data():
    global _last_art_url, _ready_cover_hash, _ready_cover_b64
    try:
        proc = subprocess.run([
            "playerctl", "metadata",
            "--format", "{{status}}:::{{artist}}:::{{title}}:::{{album}}:::{{position}}:::{{mpris:length}}:::{{mpris:artUrl}}"
        ], capture_output=True, text=True, timeout=1.0)
        if proc.returncode != 0 or not proc.stdout.strip():
            return {"isActive": False, "status": "stopped"}
        parts = proc.stdout.strip().split(":::")
        if len(parts) < 3:
            return {"isActive": False, "status": "stopped"}
        status_raw = parts[0].strip().lower()
        status = "playing" if status_raw == "playing" else ("paused" if status_raw == "paused" else "stopped")
        artist = parts[1].strip()
        title = parts[2].strip()
        album = parts[3].strip() if len(parts) > 3 else ""
        pos_ms = 0
        if len(parts) > 4 and parts[4].strip().isdigit():
            pos_ms = max(0, int(parts[4].strip()) // 1000)
        dur_ms = 0
        if len(parts) > 5 and parts[5].strip().isdigit():
            dur_ms = max(0, int(parts[5].strip()) // 1000)
        art_url = parts[6].strip() if len(parts) > 6 else ""

        cover_hash = None
        with _cover_lock:
            if art_url:
                target_hash = hex(abs(hash(art_url)))[2:]
                if art_url != _last_art_url:
                    _last_art_url = art_url
                    _ready_cover_hash = None
                    _ready_cover_b64 = None
                    threading.Thread(target=_fetch_cover_worker, args=(art_url, target_hash), daemon=True).start()
                cover_hash = _ready_cover_hash
            else:
                _last_art_url = ""
                _ready_cover_hash = None
                _ready_cover_b64 = None

        data = {
            "isActive": bool(title or artist),
            "title": title,
            "artist": artist,
            "album": album,
            "status": status,
            "positionMs": pos_ms,
            "durationMs": dur_ms,
            "platform": "player",
            "platformName": "Linux Media"
        }
        if cover_hash:
            data["coverHash"] = cover_hash
        return data
    except (subprocess.SubprocessError, OSError) as err:
        sys.stderr.write(f"MPRIS query error: {err}\n")
        return {"isActive": False, "status": "stopped"}


class MprisHandler(http.server.BaseHTTPRequestHandler):
    ACTIONS = {
        "/control/play-pause": ["playerctl", "play-pause"],
        "/control/next": ["playerctl", "next"],
        "/control/previous": ["playerctl", "previous"],
    }

    def log_message(self, format, *args):
        pass

    def do_GET(self):
        if self.path == "/now-playing":
            data = get_mpris_data()
            resp = json.dumps(data).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Content-Length", str(len(resp)))
            self.end_headers()
            self.wfile.write(resp)
        elif self.path == "/cover":
            with _cover_lock:
                b64 = _ready_cover_b64
                chash = _ready_cover_hash
            if chash and self.headers.get("If-None-Match") == chash:
                self.send_response(304)
                self.end_headers()
                return
            payload = {"coverBase64": b64} if b64 else {}
            resp = json.dumps(payload).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            if chash:
                self.send_header("ETag", chash)
            self.send_header("Content-Length", str(len(resp)))
            self.end_headers()
            self.wfile.write(resp)
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        cmd = self.ACTIONS.get(self.path)
        if cmd:
            subprocess.run(cmd, capture_output=True, timeout=1.5)
            self.send_response(200)
            self.end_headers()
            return
        if self.path == "/shutdown":
            self.send_response(200)
            self.end_headers()
            sys.exit(0)
        self.send_response(404)
        self.end_headers()


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else PORT
    tmp_dir = tempfile.gettempdir()
    port_file = os.path.join(tmp_dir, "yandexmusichud_port.txt")
    pid_file = os.path.join(tmp_dir, "yandexmusichud_pid.txt")
    try:
        server = http.server.HTTPServer(("127.0.0.1", port), MprisHandler)
    except OSError:
        server = http.server.HTTPServer(("127.0.0.1", 0), MprisHandler)
        port = server.server_port
    with open(port_file, "w", encoding="utf-8") as f:
        f.write(str(port))
    with open(pid_file, "w", encoding="utf-8") as f:
        f.write(str(os.getpid()))
    try:
        server.serve_forever()
    finally:
        for cleanup_path in (port_file, pid_file):
            try:
                if os.path.exists(cleanup_path):
                    os.remove(cleanup_path)
            except OSError as err:
                sys.stderr.write(f"Cleanup error for {cleanup_path}: {err}\n")


if __name__ == "__main__":
    main()
