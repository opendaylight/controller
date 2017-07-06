

class ProgressTrackerVr(object):

    def __init__(self, limit, default, cutoff, sumcutoff):
        self.limit = limit
        self.default = default
        self.cutoff = cutoff
        self.sumcutoff = sumcutoff
        self.closed = 0
        self.encountered = 0
        self.lastIdle = 0
        self.lastClosed = 0
        self.allowed = 0
        self.elapsed = 0
        self.previousNow = 0

    def estimate(self, opened, now):
        if opened <= self.limit / 2:
            return 0
        if opened >= self.limit:
            return self.cutoff
        capacity = 1.0 - float(opened) / self.limit
        coefficient = (0.5 - capacity) / capacity
        worked = self.elapsed + (now - self.lastIdle)
        average = self.default if self.closed == 0 else worked / self.closed
        delay = average * coefficient
        return int(min(delay, self.cutoff))

    def openTask(self, now):
        opened = self.reopenTask(now)
        estimated = self.estimate(opened, now)
        self.allowed = max(now, self.allowed) + estimated
        delay = min(self.allowed - now, self.sumcutoff)
        return delay

    def reopenTask(self, now):
        if now <= self.previousNow:
            raise RuntimeException("Now went backward from " + str(self.previousNow) + " to " + str(now))
        opened = self.encountered - self.closed
        if opened == 0:
            self.lastIdle = now
        self.encountered += 1
        return opened

    def closeTask(self, now):
        self.closed += 1
        self.lastClosed = now
        if self.closed > self.encountered:
            raise RuntimeError("More closed than encountered")
        if self.closed == self.encountered:
            self.elapsed += now - self.lastIdle
