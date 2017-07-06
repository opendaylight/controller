
from collections import deque
from Queue import PriorityQueue
import random
random.seed(0)
from ptvr import ProgressTrackerVr
from ptro import ProgressTrackerRo
# tick = nanosecond

def wait(aggregate_delay, divisor):
    rnd = random.random()
    delay = (rnd + 0.5) * aggregate_delay * divisor
    return delay

aggregate_delay = 500000
divisor = 100
tracker = ProgressTrackerRo(4000, 1000000000, 500000000, 5000000000)
events = PriorityQueue()
tasks = deque()
sleeps = deque()

class Backend(object):
    def __init__(self):
        self.tasks = 0
        self.available = 0
    def process(self, now):
        self.tasks += 1
        tasks.append((now, self.tasks))
        self.available = max(self.available, now) + wait(aggregate_delay * 2, 1)
        return self.available
    def done(self, now):
        tracker.closeTask(now)
        self.tasks -= 1
        tasks.append((now, self.tasks))

backend = Backend()

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

for _ in range(divisor):
    user = User()
    startWork = user.switch(0)
    events.put((startWork, user))
now = 0
while now < 10000000000:
    now, obj = events.get()
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
#while now < 20000000000:
#    now, obj = events.get()
#    if isinstance(obj, Backend):
#        # Not closing the task as backend is unreachable.
#        continue
#    if not isinstance(obj, User):
#        raise RuntimeError("Got unexpected type " + repr(obj))
#    if obj.working:
#        done = backend.process(now)
#        events.put((done, backend))
#    nextSwitch = obj.switch(now)
#    events.put((nextSwitch, obj))
with open("s.txt", "w") as fileout:
    while len(sleeps):
        now, sleep = sleeps.popleft()
        fileout.write(str(now) + ',' + str(sleep) + '\n')
with open("t.txt", "w") as fileout:
    while len(tasks):
        now, task = tasks.popleft()
        fileout.write(str(now) + ',' + str(task) + '\n')
