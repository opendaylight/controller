
from collections import deque
from Queue import PriorityQueue
import random
random.seed(0)
from ptbase import ProgressTrackerVr
from ptbase import ProgressTrackerRo
# tick = nanosecond

def wait(aggregate_delay, divisor):
    rnd = random.random()
    delay = (rnd + 0.5) * aggregate_delay * divisor
    return delay

aggregate_delay = 500000
divisor = 2
tracker = ProgressTrackerRo(4000, 1000000000, 500000000, 5000000000)
events = PriorityQueue()
tasks = deque()
sleeps = deque()

class Backend(object):
    def __init__(self, now):
        self.reset(now)
    def reset(self, now):
        self.tasks = 0
        self.available = now
    def process(self, now):
        self.tasks += 1
        tasks.append((now, self.tasks))
        self.available = max(self.available, now) + wait(aggregate_delay * 2, 1)
        return self.available
    def done(self, now):
        tracker.closeTask(now)
        self.tasks -= 1
        tasks.append((now, self.tasks))

class User(object):
    def __init__(self):
        self.working = False
    def switch(self, now):
        if not self.working:
            self.working = True
            work = wait(aggregate_delay, divisor)
            return now + work
        self.working = False
        sleep = tracker.openTask(now)
        sleeps.append((now, sleep))
        return now + sleep

now = 0
backend = Backend(0)
for _ in range(divisor):
    user = User()
    startWork = user.switch(0)
    events.put((startWork, user))
while 1:
    now, obj = events.get()
    if now >= 10000000000:
        events.put((now, obj))
        break
    if isinstance(obj, Backend):
        obj.done(now)
        continue
    if not isinstance(obj, User):
        raise RuntimeError("Got unexpected type " + repr(obj))
    if obj.working:
        done = backend.process(now)
        events.put((done, backend))
    nextSwitch = obj.switch(now)
    events.put((nextSwitch, obj))
while 1:
    now, obj = events.get()
    if now >= 20000000000:
        events.put((now, obj))
        break
    if isinstance(obj, Backend):
        # Not closing the task as backend is unreachable.
        continue
    if not isinstance(obj, User):
        raise RuntimeError("Got unexpected type " + repr(obj))
    if obj.working:
        done = backend.process(now)
        events.put((done, backend))
    nextSwitch = obj.switch(now)
    events.put((nextSwitch, obj))
now = 20000000000
opened = tracker.encountered - tracker.closed
backend.reset(now)
tracker.reconnect(now)
for _ in range(opened):
    tracker.openTask(now)
    done = backend.process(now)
    events.put((done, backend))
while 1:
    now, obj = events.get()
    if now >= 30000000000:
        events.put((now, obj))
        break
    if isinstance(obj, Backend):
        obj.done(now)
        continue
    if not isinstance(obj, User):
        raise RuntimeError("Got unexpected type " + repr(obj))
    if obj.working:
        done = backend.process(now)
        events.put((done, backend))
    nextSwitch = obj.switch(now)
    events.put((nextSwitch, obj))
with open("s.txt", "w") as fileout:
    while len(sleeps):
        now, sleep = sleeps.popleft()
        if now <= 20000000000:
            continue
#        if now >= 10000000000:
#            break
        fileout.write(str(now) + ',' + str(sleep) + '\n')
with open("t.txt", "w") as fileout:
    while len(tasks):
        now, task = tasks.popleft()
#        if now >= 10000000000:
#            break
        fileout.write(str(now) + ',' + str(task) + '\n')
