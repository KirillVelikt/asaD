#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Юмани Терминал — пульт управления (демо). Подключение по коду."""
import socket, threading, time, queue, random
import tkinter as tk
from tkinter import ttk
from concurrent.futures import ThreadPoolExecutor

PORT_TCP = 8090
PORT_UDP = 8091

def log(msg):
    print(msg)

def beep_ok():
    try:
        import winsound
        winsound.Beep(1200, 150); winsound.Beep(1600, 250)
    except Exception:
        print('\a')

def beep_fail():
    try:
        import winsound
        winsound.Beep(300, 400)
    except Exception:
        print('\a')

def tcp_probe(ip, code):
    try:
        s = socket.create_connection((ip, PORT_TCP), timeout=0.5)
        s.sendall(('CODE %s\n' % code).encode())
        resp = s.recv(64).decode('utf-8', 'ignore').strip()
        s.close()
        return resp == 'OK'
    except OSError:
        return False

def scan_subnet(prefix, code):
    found = []
    def probe(i):
        ip = prefix + str(i)
        if tcp_probe(ip, code):
            found.append(ip)
    with ThreadPoolExecutor(max_workers=60) as ex:
        ex.map(probe, range(1, 255))
    return found[0] if found else None

def find_terminal(code):
    # 1) слушаем UDP-маяк терминала
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try: s.bind(('', PORT_UDP))
    except OSError: pass
    s.settimeout(0.5)
    t0 = time.time()
    while time.time() - t0 < 5:
        try:
            data, addr = s.recvfrom(1024)
            msg = data.decode('utf-8', 'ignore')
            if msg.startswith('YUMANI_HERE|') and msg.split('|')[1] == code:
                s.close()
                return addr[0]
        except socket.timeout:
            continue
        except OSError:
            break
    s.close()
    # 2) сканируем типичные подсети
    for prefix in ('192.168.1.', '192.168.0.', '192.168.31.', '10.0.0.', '172.16.0.'):
        ip = scan_subnet(prefix, code)
        if ip:
            return ip
    return None

class Client:
    def __init__(self):
        self.sock = None

    def connect(self, ip, code):
        s = socket.create_connection((ip, PORT_TCP), timeout=3)
        s.sendall(('CODE %s\n' % code).encode())
        resp = s.recv(64).decode('utf-8', 'ignore').strip()
        if resp != 'OK':
            s.close(); raise RuntimeError('код не принят: ' + resp)
        self.sock = s
        return True

    def send(self, cmd):
        if not self.sock: return None
        try:
            self.sock.sendall((cmd + '\n').encode())
            return self.sock.recv(256).decode('utf-8', 'ignore').strip()
        except OSError:
            self.sock = None
            return None

    def close(self):
        if self.sock:
            try: self.sock.sendall(b'BYE\n')
            except OSError: pass
            try: self.sock.close()
            except OSError: pass
            self.sock = None

class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title('Пульт терминала Юмани (демо)')
        self.geometry('460x560')
        self.client = Client()
        self.connected_ip = None
        self.q = queue.Queue()
        self._build()
        self.after(100, self._pump)
        self._poller = None

    def _build(self):
        f = ttk.Frame(self, padding=12); f.pack(fill='both', expand=True)
        ttk.Label(f, text='Код подключения:').grid(row=0, column=0, sticky='w')
        self.e_code = ttk.Entry(f, width=14); self.e_code.grid(row=0, column=1, sticky='we', pady=4)
        ttk.Button(f, text='Подключиться', command=self.on_connect).grid(row=1, column=0, columnspan=2, sticky='we', pady=4)
        ttk.Separator(f).grid(row=2, column=0, columnspan=2, sticky='we', pady=8)
        ttk.Label(f, text='Сумма, ₽:').grid(row=3, column=0, sticky='w')
        self.e_amount = ttk.Entry(f, width=14); self.e_amount.grid(row=3, column=1, sticky='we', pady=4)
        self.btn_pay = ttk.Button(f, text='НАЧАТЬ ОПЛАТУ', command=self.on_pay, state='disabled')
        self.btn_pay.grid(row=4, column=0, columnspan=2, sticky='we', pady=4)
        self.btn_cancel = ttk.Button(f, text='Отменить операцию', command=self.on_cancel, state='disabled')
        self.btn_cancel.grid(row=5, column=0, columnspan=2, sticky='we', pady=4)
        ttk.Separator(f).grid(row=6, column=0, columnspan=2, sticky='we', pady=8)
        ttk.Label(f, text='Статус:').grid(row=7, column=0, sticky='w')
        self.l_status = ttk.Label(f, text='—'); self.l_status.grid(row=7, column=1, sticky='w')
        self.txt = tk.Text(f, height=14, state='disabled', font=('Consolas', 9))
        self.txt.grid(row=8, column=0, columnspan=2, sticky='nsew', pady=8)
        f.rowconfigure(8, weight=1); f.columnconfigure(1, weight=1)

    def log(self, m):
        self.q.put(m)

    def _pump(self):
        while not self.q.empty():
            m = self.q.get()
            self.txt['state'] = 'normal'
            self.txt.insert('end', m + '\n'); self.txt.see('end')
            self.txt['state'] = 'disabled'
        self.after(100, self._pump)

    def on_connect(self):
        code = self.e_code.get().strip()
        if len(code) != 6 or not code.isdigit():
            self.log('Код должен быть 6 цифр'); return
        self.log('Ищу терминал по коду ' + code + ' …')
        def worker():
            ip = find_terminal(code)
            if not ip:
                self.log('Терминал не найден. Проверьте: один Wi-Fi, экран телефона включён.')
                return
            try:
                self.client.connect(ip, code)
                self.connected_ip = ip
                self.log('Подключено: ' + ip)
                self.btn_pay['state'] = 'normal'
                self.btn_cancel['state'] = 'normal'
                if not self._poller:
                    self._poller = threading.Thread(target=self._poll, daemon=True)
                    self._poller.start()
            except Exception as e:
                self.log('Ошибка подключения: %s' % e)
        threading.Thread(target=worker, daemon=True).start()

    def on_pay(self):
        amount = self.e_amount.get().strip().replace(',', '.')
        try:
            v = float(amount)
            if v <= 0: raise ValueError
        except ValueError:
            self.log('Введите сумму больше нуля'); return
        resp = self.client.send('AMOUNT %.2f' % v)
        self.log('Начата оплата: %.2f ₽ (%s)' % (v, resp or 'нет ответа'))

    def on_cancel(self):
        resp = self.client.send('CANCEL')
        self.log('Отмена отправлена (%s)' % (resp or 'нет ответа'))

    def _poll(self):
        while True:
            time.sleep(1)
            if not self.client.sock: continue
            st = self.client.send('STATUS')
            if st is None:
                self.log('Соединение потеряно')
                self.btn_pay['state'] = 'disabled'
                self.btn_cancel['state'] = 'disabled'
                self.client.sock = None
                continue
            if st.startswith('PAID'):
                rrn = st.split(' ')[-1]
                self.q.put('ОПЛАТА ПРОЙДЕНА. RRN: ' + rrn)
                beep_ok()
                self.client.send('STATUS_ACK') if False else None
            elif st == 'DECLINED':
                self.q.put('ОТКАЗ в оплате')
                beep_fail()

if __name__ == '__main__':
    App().mainloop()
