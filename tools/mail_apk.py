"""릴리스 APK가 바뀔 때마다 Gmail/네이버 '나에게 보내기'로 전달한다.

처음 한 번 (터미널에서 직접 실행 — 앱 비밀번호를 묻는다):
    py tools/mail_apk.py setup gmail 내주소@gmail.com
    py tools/mail_apk.py setup naver 아이디@naver.com

그 뒤로는 `gradlew assembleRelease` 가 끝날 때 Gradle 이 자동으로 부른다:
    py tools/mail_apk.py send <APK 폴더>
직전에 보낸 것과 내용이 같은 APK 는 다시 보내지 않는다.

비밀번호는 Windows 자격 증명 관리자에 저장한다(파일에 남기지 않음).
- Gmail: 2단계 인증을 켠 뒤 https://myaccount.google.com/apppasswords 의 16자리 앱 비밀번호
- 네이버: 메일 > 환경설정 > POP3/IMAP 설정에서 'IMAP/SMTP 사용'을 켜고,
          2단계 인증을 쓰면 보안설정의 '애플리케이션 비밀번호'
"""
import ctypes
import ctypes.wintypes as wt
import getpass
import hashlib
import io
import json
import os
import smtplib
import ssl
import sys
import zipfile
from datetime import datetime
from email.message import EmailMessage
from pathlib import Path

SERVERS = {"gmail": ("smtp.gmail.com", 465), "naver": ("smtp.naver.com", 465)}
STATE_DIR = Path(os.environ.get("APPDATA", Path.home())) / "ShutterZero"
CONFIG = STATE_DIR / "mail.json"
CRED_TARGET = "ShutterZero/apk-mail"
# Gmail 첨부 한도 25MB. 여유를 둔다.
MAX_ZIP_BYTES = 24 * 1024 * 1024

# ---- Windows 자격 증명 관리자 (advapi32 CredWriteW / CredReadW) ----

class _CREDENTIAL(ctypes.Structure):
    _fields_ = [
        ("Flags", wt.DWORD), ("Type", wt.DWORD), ("TargetName", wt.LPWSTR),
        ("Comment", wt.LPWSTR), ("LastWritten", wt.FILETIME),
        ("CredentialBlobSize", wt.DWORD), ("CredentialBlob", ctypes.POINTER(ctypes.c_ubyte)),
        ("Persist", wt.DWORD), ("AttributeCount", wt.DWORD), ("Attributes", ctypes.c_void_p),
        ("TargetAlias", wt.LPWSTR), ("UserName", wt.LPWSTR),
    ]

_CRED_TYPE_GENERIC = 1
_CRED_PERSIST_LOCAL_MACHINE = 2
_advapi = ctypes.WinDLL("advapi32", use_last_error=True)


def _cred_write(user: str, secret: str) -> None:
    blob = secret.encode("utf-16-le")
    buf = (ctypes.c_ubyte * len(blob)).from_buffer_copy(blob)
    cred = _CREDENTIAL(Type=_CRED_TYPE_GENERIC, TargetName=CRED_TARGET, UserName=user,
                       CredentialBlobSize=len(blob), CredentialBlob=buf,
                       Persist=_CRED_PERSIST_LOCAL_MACHINE)
    if not _advapi.CredWriteW(ctypes.byref(cred), 0):
        raise OSError(ctypes.get_last_error(), "자격 증명을 저장하지 못했습니다")


def _cred_read() -> str | None:
    p = ctypes.POINTER(_CREDENTIAL)()
    if not _advapi.CredReadW(CRED_TARGET, _CRED_TYPE_GENERIC, 0, ctypes.byref(p)):
        return None
    try:
        c = p.contents
        return ctypes.string_at(c.CredentialBlob, c.CredentialBlobSize).decode("utf-16-le")
    finally:
        _advapi.CredFree(p)

# ---- 메일 ----


def _login(service: str, address: str, password: str) -> smtplib.SMTP_SSL:
    host, port = SERVERS[service]
    smtp = smtplib.SMTP_SSL(host, port, context=ssl.create_default_context(), timeout=60)
    smtp.login(address, password)
    return smtp


def setup(service: str, address: str) -> None:
    if service not in SERVERS:
        sys.exit("서비스는 gmail 또는 naver 중 하나입니다.")
    password = getpass.getpass(f"{address} 의 앱 비밀번호(입력해도 보이지 않음): ").replace(" ", "")
    print("로그인 확인 중...")
    _login(service, address, password).quit()
    _cred_write(address, password)
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    cfg = json.loads(CONFIG.read_text("utf-8")) if CONFIG.exists() else {}
    cfg.update(service=service, address=address)
    CONFIG.write_text(json.dumps(cfg, ensure_ascii=False, indent=2), "utf-8")
    print(f"설정 완료: 릴리스 빌드마다 {address} 로 '나에게 보내기' 합니다.")


def _newest_apk(path: Path) -> Path:
    if path.is_file():
        return path
    apks = sorted(path.glob("*.apk"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not apks:
        raise FileNotFoundError(f"APK가 없습니다: {path}")
    return apks[0]


def _warn(msg: str) -> None:
    # Gradle -q 에서도 보이도록 stderr 로
    print(f"[메일 미발송] {msg}", file=sys.stderr)


def send(target: str) -> int:
    if not CONFIG.exists():
        _warn("메일 설정이 없습니다. 터미널에서 `py tools/mail_apk.py setup gmail 주소` 를 한 번 실행해 주세요.")
        return 0
    cfg = json.loads(CONFIG.read_text("utf-8"))
    password = _cred_read()
    if not password:
        _warn("저장된 앱 비밀번호가 없습니다. setup 을 다시 실행해 주세요.")
        return 0

    apk = _newest_apk(Path(target))
    data = apk.read_bytes()
    digest = hashlib.sha256(data).hexdigest()
    if cfg.get("last_sha256") == digest:
        print(f"[메일] {apk.name} 은 직전에 보낸 것과 같아 건너뜁니다.")
        return 0

    # Gmail 은 zip 안의 .apk 까지 차단하므로 확장자를 .apk_ 로 바꿔 넣는다.
    inner = apk.name + "_"
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        z.writestr(inner, data)
    zbytes = buf.getvalue()
    zname = apk.stem + ".zip"
    if len(zbytes) > MAX_ZIP_BYTES:
        _warn(f"zip 이 {len(zbytes) / 1e6:.1f}MB 로 첨부 한도를 넘습니다.")
        return 1

    built = datetime.fromtimestamp(apk.stat().st_mtime).strftime("%Y-%m-%d %H:%M")
    msg = EmailMessage()
    msg["Subject"] = f"[셔터 제로] {apk.stem} ({built})"
    msg["From"] = cfg["address"]
    msg["To"] = cfg["address"]
    msg.set_content(
        f"새 빌드: {apk.name}\n빌드 시각: {built}\nSHA-256: {digest}\n\n"
        "설치: 첨부 zip 을 폰에서 풀고, 나온 파일 이름 끝의 '_' 를 지워 .apk 로 바꾼 뒤 누르세요.\n"
        "(Gmail 이 zip 속 .apk 를 차단해서 이름을 바꿔 보냅니다.)\n"
        "같은 서명이라 기존 앱 위에 덮어 설치되고, 설정과 권한은 유지됩니다.\n"
    )
    msg.add_attachment(zbytes, maintype="application", subtype="zip", filename=zname)
    try:
        with _login(cfg["service"], cfg["address"], password) as smtp:
            smtp.send_message(msg)
    except Exception as e:  # 빌드는 깨지 않되 실패는 분명히 알린다
        _warn(f"발송 실패: {type(e).__name__}: {e}")
        return 1

    cfg.update(last_sha256=digest, last_sent=datetime.now().isoformat(timespec="seconds"), last_file=apk.name)
    CONFIG.write_text(json.dumps(cfg, ensure_ascii=False, indent=2), "utf-8")
    print(f"[메일] {apk.name} → {cfg['address']} 보냄 ({len(zbytes) / 1e6:.1f}MB)")
    return 0


def main() -> None:
    args = sys.argv[1:]
    if len(args) == 3 and args[0] == "setup":
        setup(args[1], args[2])
    elif len(args) == 2 and args[0] == "send":
        sys.exit(send(args[1]))
    else:
        print(__doc__)
        sys.exit(2)


if __name__ == "__main__":
    # Gradle/콘솔 코드페이지와 무관하게 한글이 깨지지 않도록
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8")
        except Exception:
            pass
    main()
