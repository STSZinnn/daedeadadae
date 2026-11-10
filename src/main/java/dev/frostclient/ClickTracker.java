package dev.frostclient;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ClickTracker {
    private final Deque<Long> left = new ArrayDeque<>();
    private final Deque<Long> right = new ArrayDeque<>();

    public void record(boolean leftClick) {
        long now = System.currentTimeMillis();
        Deque<Long> queue = leftClick ? left : right;
        if(queue.isEmpty() || now-queue.peekLast()>35L) queue.addLast(now);
        trim(queue, now);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        trim(left, now);
        trim(right, now);
    }

    public int leftCps() { return left.size(); }
    public int rightCps() { return right.size(); }

    private static void trim(Deque<Long> queue, long now) {
        while (!queue.isEmpty() && now - queue.peekFirst() > 1000L) queue.removeFirst();
    }
}
