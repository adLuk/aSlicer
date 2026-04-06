package cz.ad.print3d.aslicer.logic.net.scanner.dto;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A memory-efficient Set implementation for a range of ports.
 * It avoids the overhead of storing 65535 Integer objects and Set nodes.
 */
public class PortRangeSet extends AbstractSet<Integer> {
    private final int start;
    private final int end;

    /**
     * Constructs a new PortRangeSet.
     *
     * @param start the start port (inclusive)
     * @param end   the end port (inclusive)
     */
    public PortRangeSet(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public int size() {
        return Math.max(0, end - start + 1);
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Integer)) return false;
        int p = (Integer) o;
        return p >= start && p <= end;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int current = start;

            @Override
            public boolean hasNext() {
                return current <= end;
            }

            @Override
            public Integer next() {
                if (!hasNext()) throw new NoSuchElementException();
                return current++;
            }
        };
    }
}
