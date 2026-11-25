# -*- coding: utf-8 -*-
"""
Загружает все файлы из текущей папки на GitHub с повторами при сбоях сети.
Запуск:  py new.py
"""
import subprocess
import sys
import time

GIT_USER = "KirillVelikt"
REPO_NAME = "Tetsst"
TOKEN = ""
BRANCH = "main"
MAX_RETRIES = 5


def run(cmd):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True,
                          encoding="utf-8", errors="replace")


def success():
    print("\nГОТОВО! Все файлы на GitHub.")
    print(f"Ссылка: https://github.com/{GIT_USER}/{REPO_NAME}")
    print("Дальше: вкладка Actions -> зелёные галочки ->")
    print("скачай app-debug-apk, TerminalControl-EXE и TerminalControl-Installer.")


def main():
    token = TOKEN or input("Вставь Personal Access Token с GitHub: ").strip()
    if not token:
        print("Токен обязателен. Выход.")
        sys.exit(1)

    remote = f"https://{GIT_USER}:{token}@github.com/{GIT_USER}/{REPO_NAME}.git"

    def step(desc, cmd, ignore_errors=False):
        print(f"--> {desc} ...")
        r = run(cmd)
        if r.returncode != 0 and not ignore_errors:
            out = (r.stdout + r.stderr).strip()
            if ("already exists" not in out and "nothing to commit" not in out
                    and "nothing added" not in out):
                print(out)
                print("Останов.")
                sys.exit(1)

    step("Проверка git", "git --version")
    step("Инициализация репозитория", "git init")
    step("Ветка main", f"git branch -M {BRANCH}", ignore_errors=True)
    step("Настройка имени", f'git config user.name "{GIT_USER}"')
    step("Настройка почты", f'git config user.email "{GIT_USER}@users.noreply.github.com"')
    step("Удаление старого origin", "git remote remove origin", ignore_errors=True)
    step("Привязка origin", f"git remote add origin {remote}")
    step("Добавление файлов", "git add -A")
    step("Коммит", 'git commit -m "Терминал Юмани: пульт, улыбка, QR, установщик"', ignore_errors=True)

    def push(force=False):
        flag = "-f " if force else "-u "
        label = "Принудительный пуш" if force else "Пуш"
        for attempt in range(1, MAX_RETRIES + 1):
            print(f"--> {label} (попытка {attempt}/{MAX_RETRIES}) ...")
            r = run(f"git push {flag}origin {BRANCH}")
            print((r.stdout + r.stderr).strip())
            if r.returncode == 0:
                return True
            if attempt < MAX_RETRIES:
                wait = 5 * attempt
                print(f"Сбой соединения. Повтор через {wait} сек...")
                time.sleep(wait)
        return False

    if push():
        success()
        return

    out_last = (run(f"git push -u origin {BRANCH}").stdout
                + run(f"git push -u origin {BRANCH}").stderr)
    if "rejected" in out_last.lower() or "fetch first" in out_last.lower():
        print("На GitHub есть коммиты, которых нет локально. Перезаписываю...")
        if push(force=True):
            success()
            return

    print("\nНе удалось запушить после всех попыток.")
    print("Проверь githubstatus.com или попробуй раздать интернет с телефона.")
    sys.exit(1)


if __name__ == "__main__":
    main()
