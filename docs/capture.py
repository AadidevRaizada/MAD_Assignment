#!/usr/bin/env python3
"""Drive the N152 To-Do app on an emulator and capture one screenshot per CRUD step.

Coordinates are never hard-coded: every tap is resolved from a fresh uiautomator dump,
so the script survives the dialog moving when the keyboard opens.
"""
import re, subprocess, sys, time, os

ADB = "/opt/homebrew/share/android-commandlinetools/platform-tools/adb"
PKG = "com.mad.n152.todo"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screenshots")
os.makedirs(OUT, exist_ok=True)


def adb(*args, binary=False, tries=5):
    for attempt in range(tries):
        p = subprocess.run([ADB, *args], capture_output=True)
        err = p.stderr.decode(errors="replace")
        if p.returncode == 0 and "offline" not in err and "not found" not in err:
            return p.stdout if binary else p.stdout.decode(errors="replace")
        subprocess.run([ADB, "wait-for-device"], capture_output=True, timeout=120)
        time.sleep(1.5)
    raise SystemExit("adb failed: %s %s" % (args, err))


def sh(cmd):
    return adb("shell", cmd)


def dump():
    """uiautomator refuses to dump while the window is animating, so retry."""
    xml = ""
    for _ in range(10):
        subprocess.run([ADB, "shell", "rm -f /sdcard/ui.xml"], capture_output=True)
        subprocess.run([ADB, "shell", "uiautomator dump /sdcard/ui.xml"],
                       capture_output=True, timeout=60)
        got = subprocess.run([ADB, "shell", "cat /sdcard/ui.xml"], capture_output=True)
        xml = got.stdout.decode(errors="replace")
        if "<node" in xml:
            break
        time.sleep(1.5)
    else:
        raise SystemExit("uiautomator never produced a dump")
    nodes = []
    for m in re.finditer(r"<node[^>]*>", xml):
        s = m.group(0)
        text = (re.search(r'text="([^"]*)"', s) or [None, ""])[1]
        desc = (re.search(r'content-desc="([^"]*)"', s) or [None, ""])[1]
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', s)
        if not b:
            continue
        x1, y1, x2, y2 = map(int, b.groups())
        nodes.append({"text": text, "desc": desc,
                      "box": (x1, y1, x2, y2),
                      "center": ((x1 + x2) // 2, (y1 + y2) // 2)})
    return nodes


def find(label, nodes=None, exact=False):
    nodes = nodes if nodes is not None else dump()
    for n in nodes:
        for field in ("text", "desc"):
            v = n[field]
            if (v == label) if exact else (label in v):
                return n
    return None


def need(label, **kw):
    n = find(label, **kw)
    if not n:
        raise SystemExit("not on screen: " + label)
    return n


def tap(label, **kw):
    x, y = need(label, **kw)["center"]
    sh("input tap %d %d" % (x, y))
    time.sleep(1.2)


def tap_xy(x, y, pause=1.2):
    sh("input tap %d %d" % (x, y))
    time.sleep(pause)


def wait_for(label, timeout=20, exact=False):
    """Poll the view hierarchy until `label` shows up (dialogs animate in)."""
    deadline = time.time() + timeout
    while time.time() < deadline:
        n = find(label, exact=exact)
        if n:
            return n
        time.sleep(1)
    raise SystemExit("never appeared: " + label)


def tap_until(label, expect, tries=4, exact=False, expect_exact=False):
    """Tap `label`, and retry while `expect` has not shown up."""
    for _ in range(tries):
        tap(label, exact=exact)
        time.sleep(0.8)
        if find(expect, exact=expect_exact):
            return
        time.sleep(1.2)
    raise SystemExit("tapping %r never produced %r" % (label, expect))


def shot(name):
    for _ in range(5):
        data = adb("exec-out", "screencap", "-p", binary=True)
        if data.startswith(b"\x89PNG") and len(data) > 10000:
            break
        time.sleep(1.5)
    else:
        raise SystemExit("screencap kept failing for " + name)
    path = os.path.join(OUT, name + ".png")
    with open(path, "wb") as f:
        f.write(data)
    print("  captured", name, len(data), "bytes")
    return path


def type_text(s):
    # One `input text` for the whole string races with the IME and interleaves
    # characters, so send it a word at a time and let each land.
    for i, word in enumerate(s.split(" ")):
        sh("input text '%s%s'" % ("%s" if i else "", word))
        time.sleep(0.6)
    time.sleep(0.6)
    sh("input keyevent 4")          # BACK closes the IME, not the dialog
    time.sleep(1.2)


def add_task(description, priority, shots=()):
    tap_until("Add task", "New task")            # the FAB opens the editor dialog
    if "dialog-blank" in shots:
        shot("02-add-dialog-blank")
    field = wait_for("Description")
    tap_xy(field["center"][0], field["center"][1])
    type_text(description)
    wait_for(priority, exact=True)
    tap(priority, exact=True)
    if "dialog-filled" in shots:
        shot("03-add-dialog-filled")
    wait_for("Add", exact=True)
    tap("Add", exact=True)
    time.sleep(2.5)


# ONLY=delete re-runs just the delete capture against an already-populated list.
if os.environ.get("ONLY") != "delete":
    print("== restarting the app ==")
    sh("am force-stop " + PKG)
    adb("shell", "am", "start", "-n", PKG + "/.MainActivity")
    time.sleep(5)
    shot("01-empty-state")

    print("== CREATE ==")
    add_task("Submit Firebase assignment", "High", shots=("dialog-blank", "dialog-filled"))
    shot("04-first-task-created")
    add_task("Revise Realtime Database rules", "Medium")
    add_task("Back up lab screenshots", "Low")
    add_task("Buy a new USB-C cable", "Low")
    print("== READ: sorted list ==")
    shot("05-list-sorted-by-priority")

    print("== UPDATE ==")
    wait_for("Revise Realtime Database rules")
    for attempt in range(4):
        nodes = dump()
        row = need("Revise Realtime Database rules", nodes=nodes)
        edits = [n for n in nodes if n["desc"] == "Edit task"]
        target = min(edits, key=lambda n: abs(n["center"][1] - row["center"][1]))
        tap_xy(*target["center"])
        time.sleep(1.5)
        if find("Edit task", exact=True):   # the dialog title
            break
    else:
        raise SystemExit("edit dialog never opened")
    shot("06-edit-dialog")
    tap("High", exact=True)
    time.sleep(0.8)
    shot("07-edit-dialog-priority-changed")
    wait_for("Save", exact=True)
    tap("Save", exact=True)
    time.sleep(3)
    shot("08-list-after-update")

print("== DELETE ==")
wait_for("Buy a new USB-C cable")
nodes = dump()
row = need("Buy a new USB-C cable", nodes=nodes)
# `row` is the description TextView, which is narrower than the card. The
# gesture has to cross half the CARD's width, so drive it across the screen.
_x1, y1, _x2, y2 = row["box"]
y = (y1 + y2) // 2
screen_w = int(re.search(r"(\d+)x\d+", sh("wm size")).group(1))
# Stay clear of both screen edges: a drag that starts within ~48dp of the right
# edge is swallowed by the system back gesture and closes the app instead.
x1, x2 = 150, screen_w - 150


def drag(from_x, to_x, y, steps=18, hold=False):
    """`input swipe` emits too few MOVE events for Compose's drag detector, so
    synthesise the gesture one motion event at a time."""
    sh("input motionevent DOWN %d %d" % (from_x, y))
    for i in range(1, steps + 1):
        x = int(from_x + (to_x - from_x) * i / steps)
        sh("input motionevent MOVE %d %d" % (x, y))
    if hold:
        return
    sh("input motionevent UP %d %d" % (to_x, y))


# Hold the row a third of the way across so the red delete panel is visible.
drag(x2, x2 - (x2 - x1) // 3, y, hold=True)
shot("09-swipe-reveals-delete")
sh("input motionevent UP %d %d" % (x2 - (x2 - x1) // 3, y))
time.sleep(2)

for _ in range(4):
    drag(x2, x1, y, steps=26)
    time.sleep(3)
    nodes = dump()
    if not find("N152 To-Do", nodes=nodes):
        raise SystemExit("the app is no longer in the foreground")
    if not find("Buy a new USB-C cable", nodes=nodes):
        break
else:
    raise SystemExit("swipe never deleted the row")
shot("10-list-after-delete")

print("done")
