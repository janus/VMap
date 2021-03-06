package com.ociweb.collection;

import com.ociweb.lang.MutableInteger;
import java.util.Iterator;

/**
 * A versioned, logically immutable hash set.
 * @author R. Mark Volkmann, Object Computing, Inc.
 * @param <V> the value type
 */
public class VHashSet<V> implements VSet<V> {

    private MutableInteger highestVersion = new MutableInteger();
    private InternalSet<V> set;
    private Version unusedVersion;
    private Version version;
    //private VHashSet<V> parent; // TODO: Want this?
    private int size;

    /**
     * Creates an empty VHashSet with the default initial capacity.
     */
    public VHashSet() {
        version = new Version();
        set = new InternalSet<V>(InternalSet.INITIAL_BUCKET_COUNT);
    }

    /**
     * Creates an empty VHashSet with a given initial capacity.
     * @param initialCapacity the initial capacity
     */
    public VHashSet(int initialCapacity) {
        version = new Version();
        set = new InternalSet<V>(initialCapacity);
    }

    /**
     * Creates a VHashSet with given compatible values.
     * @param values any number of compatible values
     */
    public VHashSet(V... values) {
        version = new Version();
        set = new InternalSet<V>(values.length);
        size = set.add(version, values);
    }

    /**
     * Creates the next version of a given VHashSet.
     * @param parent the parent VHashSet to the new one
     * @param version the version of the new one
     */
    private VHashSet(VHashSet<V> parent, Version version) {
        synchronized (this) {
            // Share internal set with parent version.
            set = parent.set;

            size = parent.size;
            highestVersion = parent.highestVersion;
            this.version = version;
            //this.parent = parent;
        }
    }

    // Javadoc comes from the VSet interface.
    @Override
    public final synchronized VSet<V> add(V... values) {
        Version nextVersion = getNextVersion();
        int addedCount = set.add(nextVersion, values);
        if (addedCount == 0) {
            unusedVersion = nextVersion;
            return this;
        }

        VHashSet<V> newSet = new VHashSet<V>(this, nextVersion);
        newSet.size += addedCount;
        unusedVersion = null;
        return newSet;
    }

    // Javadoc comes from the VSet interface.
    @Override
    public final synchronized boolean contains(V value) {
        return set.contains(version, value);
    }


    // Javadoc comes from the VSet interface.
    @Override
    public final synchronized VSet<V> delete(V... values) {
        Version nextVersion = getNextVersion();
        int deletedCount = set.delete(nextVersion, values);
        if (deletedCount == 0) {
            unusedVersion = nextVersion;
            return this;
        }

        VHashSet<V> newSet = new VHashSet<V>(this, nextVersion);
        newSet.size -= deletedCount;
        unusedVersion = null;
        return newSet;
    }

    // Javadoc comes from the VSet interface.
    @Override
    public final synchronized void dump(String name, boolean includeContent) {
        System.out.println("\n<<< start of VHashSet dump for " + name);
        System.out.println("size = " + size());
        System.out.println(this);
        set.dump(includeContent);
        System.out.println(">>> end of VHashSet dump for " + name + '\n');
    }

    /**
     * Indicates whether some object is "equal" to this one.
     * @param obj the object with which to compare
     * @return true if equal; false otherwise
     */
    @Override
    public final boolean equals(Object obj) {
        // Next line makes NetBeans happy.
        if (!(obj instanceof VHashSet)) return false;
        return obj == this;
    }

    /**
     * Gets the next version of this set that can be created.
     * @return the Version
     */
    private Version getNextVersion() {
        if (unusedVersion != null) return unusedVersion;
        if (version.number == Integer.MAX_VALUE) throw new VersionException();
        return new Version(highestVersion, version);
    }

    // Javadoc comes from the VSet interface.
    @Override
    public final int getVersionNumber() { return version.number; }

    /**
     * Throws UnsupportedOperationException because
     * VHashMap objects cannot be used as keys in hash tables.
     */
    @Override
    public final int hashCode() {
        throw new UnsupportedOperationException(
            "cannot use as a key in a map or set");
    }

    // Javadoc comes from the VSet interface.
    @Override
    public final Iterator<V> iterator() {
        return new VHashSetIterator<V>();
    }

    // Javadoc comes from the VSet interface.
    @Override
    public final int size() { return size; }

    /**
     * Gets the string representation of this object.
     * @return the string representation
     */
    @Override
    public final String toString() {
        return "VHashSet: " + version + ", size=" + size;
    }

    /**
     * An Iterator for iterating through the values of this set.
     * @param <V> the value type
     */
    class VHashSetIterator<V> implements Iterator<V> {

        private Iterator<VSetEntry> iterator = set.iterator(version);

        /**
         * Determines whether that is another value to visit.
         * @return true if so; false otherwise
         */
        @Override
        public boolean hasNext() { return iterator.hasNext(); }

        /**
         * Gets the next value.
         * @return the next value
         */
        @Override
        public V next() {
            @SuppressWarnings("unchecked")
            VSetEntry<V> entry = iterator.next();
            return entry == null ? null : entry.value;
        }

        /**
         * Removing entries from this iterator is not supported.
         */
        @Override
        public void remove() { iterator.remove(); }
    }
}