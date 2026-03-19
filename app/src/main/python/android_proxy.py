from __future__ import annotations

import asyncio
import logging
import threading
import time
from typing import List, Optional, Any

from proxy import tg_ws_proxy


_thread: Optional[threading.Thread] = None
_loop: Optional[asyncio.AbstractEventLoop] = None
_stop_event: Optional[asyncio.Event] = None
_lock = threading.Lock()
_last_error: Optional[str] = None


def _normalize_dc_ip_list(dc_ip_list: Any) -> List[str]:
    if dc_ip_list is None:
        return ["2:149.154.167.220", "4:149.154.167.220"]
    if isinstance(dc_ip_list, str):
        parts = [p.strip() for p in dc_ip_list.replace(";", ",").split(",")]
        return [p for p in parts if p]
    try:
        return [str(x) for x in list(dc_ip_list)]
    except Exception:
        return [str(dc_ip_list)]


def _runner(port: int, host: str, dc_ip_list: Any, verbose: bool):
    global _loop, _stop_event, _thread, _last_error
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    stop_event = asyncio.Event()
    _loop = loop
    _stop_event = stop_event

    logging.basicConfig(
        level=logging.DEBUG if verbose else logging.INFO,
        format="%(asctime)s  %(levelname)-5s  %(name)s  %(message)s",
    )
    dc_opt = tg_ws_proxy.parse_dc_ip_list(_normalize_dc_ip_list(dc_ip_list))

    try:
        try:
            loop.run_until_complete(
                tg_ws_proxy._run(port, dc_opt, stop_event=stop_event, host=host)
            )
        except Exception as exc:
            _last_error = f"{type(exc).__name__}: {exc}"
    finally:
        try:
            loop.stop()
        except Exception:
            pass
        loop.close()
        with _lock:
            _loop = None
            _stop_event = None
            _thread = None


def start(
    port: int = 1080,
    host: str = "127.0.0.1",
    dc_ip_list: Optional[Any] = None,
    verbose: bool = False,
) -> bool:
    global _thread, _last_error
    with _lock:
        if _thread and _thread.is_alive():
            return True
        _last_error = None
        dc_ip_list = _normalize_dc_ip_list(dc_ip_list)
        _thread = threading.Thread(
            target=_runner,
            args=(port, host, dc_ip_list, verbose),
            daemon=True,
            name="tg-ws-proxy-android",
        )
        _thread.start()
    for _ in range(20):
        if is_running():
            return True
        time.sleep(0.05)
    return is_running()


def stop() -> bool:
    with _lock:
        thread = _thread
        loop = _loop
        stop_event = _stop_event
    if not thread:
        return True
    if loop and stop_event:
        loop.call_soon_threadsafe(stop_event.set)
    thread.join(timeout=5.0)
    return not thread.is_alive()


def is_running() -> bool:
    with _lock:
        return bool(_thread and _thread.is_alive())


def last_error() -> str:
    with _lock:
        return _last_error or ""
