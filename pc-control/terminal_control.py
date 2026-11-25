# -*- coding: utf-8 -*-
"""
Пульт Юмани — управление терминалом с ПК.
Подключение: по 6-значному коду (UDP-маяк, а если не нашлось — автоскан сети).
Оплата: ввод суммы -> на терминале открывается экран оплаты (улыбка / QR).
"""
import json
import socket
import subprocess
import threading
import time
import tkinter as tk
from tkinter import ttk

HTTP_PORT = 8090
BEACON_PORT = 45665

connected_ip = None
polling = False


def send_cmd(ip, payload, timeout=3):
    try:
        s = socket.create_connection((ip, HTTP_PORT), timeout=timeout)
        s.sendall(payload.encode())
        data = b""
        s.settimeout(3)
        while True:
            chunk = s.recv(4096)
            if not chunk:
                break
            data += chunk
            if data.endswith(b"}"):
                break
        s.close()
        return json.loads(data.decode())
    except Exception:
        return None


def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return None


def find_by_code(code, log):
    log("ищу маяк…")
    try:
        ds = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        ds.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        ds.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        ds.bind(("", BEACON_PORT))
        ds.settimeout(1)
        end = time.time() + 4
        while time.time() < end:
            try:
                data, addr = ds.recvfrom(1024)
                info = json.loads(data.decode())
                if info.get("code") == code:
                    return addr[0]
            except Exception:
                pass
    except Exception:
        pass

    local_ip = get_local_ip()
    if not local_ip:
        return None
    prefix = ".".join(local_ip.split(".")[:3])
    log(f"сканирую {prefix}.1-254 …")
    for i in range(1, 255):
        ip = f"{prefix}.{i}"
        resp = send_cmd(ip, json.dumps({"cmd": "ping"}), timeout=0.25)
        if resp and resp.get("code") == code:
            return ip
    return None


def play(sound):
    try:
        import winsound
        if sound == "success":
            winsound.MessageBeep(winsound.MB_ICONASTERISK)
        elif sound == "fail":
            winsound.MessageBeep(winsound.MB_ICONHAND)
        else:
            winsound.MessageBeep(winsound.MB_OK)
    except Exception:
        print("\a")


class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Пульт Юмани — терминал")
        self.geometry("420x460")
        self.configure(bg="#16221d")
        self.resizable(False, False)

        style = ttk.Style(self)
        style.theme_use("clam")

        tk.Label(self, text="юмани · пульт", bg="#16221d", fg="#b8ffffff",
                 font=("Segoe UI", 16, "bold")).pack(pady=(18, 4))

        row = tk.Frame(self, bg="#16221d")
        row.pack(pady=8)
        tk.Label(row, text="код терминала:", bg="#16221d", fg="#ffffff").pack(side="left", padx=4)
        self.e_code = tk.Entry(row, width=10, font=("Consolas", 16), justify="center")
        self.e_code.pack(side="left", padx=4)

        self.btn_connect = tk.Button(self, text="Подключить", bg="#4ce05b", fg="#14361b",
                                     font=("Segoe UI", 11, "bold"), relief="flat",
                                     command=self.connect)
        self.btn_connect.pack(pady=6, ipadx=20)

        self.lbl_status = tk.Label(self, text="не подключено", bg="#16221d", fg="#88ffffff")
        self.lbl_status.pack(pady=4)

        # блок оплаты
        paybox = tk.Frame(self, bg="#16221d")
        paybox.pack(pady=14)

        tk.Label(paybox, text="сумма, ₽:", bg="#16221d", fg="#ffffff").pack()
        self.e_amount = tk.Entry(paybox, width=12, font=("Consolas", 16), justify="center")
        self.e_amount.insert(0, "198")
        self.e_amount.pack(pady=4)

        self.btn_pay = tk.Button(paybox, text="НАЧАТЬ ОПЛАТУ", bg="#4ce05b", fg="#14361b",
                                 font=("Segoe UI", 12, "bold"), relief="flat",
                                 state="disabled", command=self.start_pay)
        self.btn_pay.pack(pady=6, ipadx=24, ipady=4)

        self.lbl_result = tk.Label(self, text="", bg="#16221d", fg="#7cfc9b",
                                   font=("Segoe UI", 13, "bold"), wraplength=380)
        self.lbl_result.pack(pady=8)

        self.btn_disconnect = tk.Button(self, text="Отключиться", bg="#2a2e33", fg="#ffffff",
                                        relief="flat", state="disabled", command=self.disconnect)
        self.btn_disconnect.pack(pady=4, ipadx=12)

    def log(self, text):
        self.lbl_status.config(text=text)

    def connect(self):
        code = self.e_code.get().strip()
        if len(code) != 6 or not code.isdigit():
            self.log("код: 6 цифр!")
            return
        self.btn_connect.config(state="disabled")
        self.log("подключаюсь…")
        threading.Thread(target=self._connect_worker, args=(code,), daemon=True).start()

    def _connect_worker(self, code):
        global connected_ip
        ip = find_by_code(code, lambda t: self.after(0, self.log, t))
        if not ip:
            self.after(0, lambda: self.log("терминал не найден"))
            self.after(0, lambda: self.btn_connect.config(state="normal"))
            return
        resp = send_cmd(ip, json.dumps({"cmd": "ping"}))
        if resp and resp.get("code") == code:
            connected_ip = ip
            self.after(0, self._on_connected)
        else:
            self.after(0, lambda: self.log("терминал не отвечает"))
            self.after(0, lambda: self.btn_connect.config(state="normal"))

    def _on_connected(self):
        global polling
        self.log(f"подключено ✓  ({connected_ip})")
        self.btn_connect.config(state="disabled")
        self.btn_pay.config(state="normal")
        self.btn_disconnect.config(state="normal")
        play("ok")
        polling = True
        threading.Thread(target=self._poll_worker, daemon=True).start()

    def start_pay(self):
        try:
            amount = int(self.e_amount.get().strip())
        except ValueError:
            self.log("сумма: только цифры!")
            return
        resp = send_cmd(connected_ip, json.dumps({"cmd": "pay", "amount": amount}))
        if resp and resp.get("status") == "started":
            self.log(f"оплата {amount} ₽ запущена на терминале")
            self.lbl_result.config(text="ждём результат с терминала…", fg="#b8ffffff")
        else:
            self.log("терминал не ответил на команду оплаты")

    def _poll_worker(self):
        global polling
        last = ""
        while polling:
            resp = send_cmd(connected_ip, json.dumps({"cmd": "result"}), timeout=2)
            if resp:
                res = resp.get("result", "waiting")
                if res != last:
                    last = res
                    self.after(0, self._show_result, res)
            time.sleep(1)

    def _show_result(self, res):
        if res == "success":
            self.lbl_result.config(text="ОПЛАТА ПРОШЛА ✓", fg="#7cfc9b")
            play("success")
        elif res == "fail":
            self.lbl_result.config(text="оплата не прошла", fg="#ff8a80")
            play("fail")
        elif res == "active":
            self.lbl_result.config(text="идёт оплата на терминале…", fg="#b8ffffff")

    def disconnect(self):
        global polling, connected_ip
        polling = False
        if connected_ip:
            send_cmd(connected_ip, json.dumps({"cmd": "close"}))
        connected_ip = None
        self.lbl_result.config(text="")
        self.log("отключено")
        self.btn_connect.config(state="normal")
        self.btn_pay.config(state="disabled")
        self.btn_disconnect.config(state="disabled")


if __name__ == "__main__":
    App().mainloop()
