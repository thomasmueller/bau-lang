package org.bau.traits;

import java.util.BitSet;

import org.bau.stdlib.util.Hash;
import org.junit.Test;

/*

A (Possibly New?) Approach to Dynamic Dispatch via Traits

In my language, I want to support dynamic dispatch through traits
(think: interfaces in Java, protocols in Swift, prototypes in JS).
I’ll call them "traits" here.

From real-world code I care about, I’ve observed:

* Most traits are small — very few have > 32 methods,
  though some rare ones have up to 200.
  We can ignore "marker traits".

* Most types implement few traits — often none; the distribution is Zipf-like.
  In one large project (Apache Jackrabbit Oak), the max is 7 traits per type.

* I want fast instanceof checks for both traits and concrete types.

* Traits can require/extend other traits
  (e.g., ReaderWriter requires Reader).

== My Dispatch Plan

I’d like feedback on this idea — does anyone see pitfalls?

1. Compile-time slot assignment

   * Each trait gets a unique ID and a slot number.
   * Two traits can share the same slot unless they appear together on a type.
   * Most traits end up in slot 0 (in Java’s JDK: ~78% in slot 0, 13% in slot 1, max slot = 17).
   * Downside: all types/traits must be known at compile-time — acceptable for my use case.

2. Object layout

   * Every object has a pointer to type metadata
     as the first field
     (no fat pointers to avoids concurrency issues).

3. Type metadata layout

    Contains:
    * One vtable with all trait functions, grouped / ordered by trait slot.
    * An “offset” array to locate the first function for a trait slot.
      Simulations show ~66% fill rate for the offset array.

== How Calls Work

At compile time, both the slot and traitFunctionId are known.

  * Trait slot 0 (~78%): vtable[traitFunctionId]
  * Trait slot >0: vtable[offset[slot] + traitFunctionId]

Most calls hit slot 0, so dispatch is very simple —
potentially competitive with Java/C++.

This is similar to Argentum’s approach but a bit simpler (no perfect hash tables).
https://aglang.org/how-the-argentum-language-makes-fast-dynamic_cast-and-method-dispatch-with-just-four-processor-instructions/

instanceof / Trait Casting

A secondary structure holds an array of trait ID for each slot.
Check: slot < traitIdArray.length && traitIdArray[slot] == traitId.


Question: Am I overlooking obvious issues here —
either in performance characteristics or in edge cases with trait inheritance?
Are there other languages you know that use this or a similar approach?

------------------



Classes implementing 0 interfaces with methods: 83.4% (4,882 classes)
Classes implementing at most 1 interface with methods: 97.0% (5,677 classes)
Classes implementing at most 2 interfaces with methods: 99.4% (5,819 classes)
Classes implementing at most 3 interfaces with methods: 99.8% (5,843 classes)
Classes implementing at most 4 interfaces with methods: 99.9% (5,849 classes)

Rank    Implementations Methods Interface Name
1   51  1   org.apache.jackrabbit.oak.run.commons.Command
2   26  0   org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants
3   26  0   org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants
4   24  6   org.apache.jackrabbit.oak.spi.state.NodeStateDiff
5   20  19  org.apache.jackrabbit.oak.spi.security.SecurityConfiguration
6   20  8   org.apache.jackrabbit.oak.spi.commit.Editor
7   19  0   org.apache.jackrabbit.oak.spi.security.user.UserConstants
8   18  1   org.apache.jackrabbit.oak.spi.commit.Observer
9   13  2   org.apache.jackrabbit.oak.spi.lifecycle.RepositoryInitializer
10  13  1   org.apache.jackrabbit.oak.spi.query.QueryIndexProvider
11  13  1   org.apache.jackrabbit.oak.spi.commit.CommitHook
12  13  3   org.apache.jackrabbit.oak.index.indexer.document.flatfile.analysis.modules.StatsCollector
13  13  0   org.apache.jackrabbit.oak.spi.security.authorization.permission.PermissionConstants
14  12  7   org.apache.jackrabbit.oak.spi.security.authorization.permission.PermissionProvider
15  12  1   org.apache.jackrabbit.oak.plugins.document.Callable
16  12  0   org.apache.lucene.util.Attribute
17  11  0   org.apache.jackrabbit.oak.spi.security.authorization.cug.impl.CugConstants
18  10  79  org.apache.jackrabbit.oak.spi.query.QueryIndex
19  10  1   org.apache.jackrabbit.oak.spi.commit.EditorProvider
20  10  7   org.apache.jackrabbit.oak.spi.blob.BlobStore

Total classes analyzed: 5,855
Classes implementing no interface methods: 4,882 (83.4%)
Classes implementing at least one interface method: 973 (16.6%)

* Most interfaces have few methods:

Based on the analysis of 973 classes that implement interfaces (this is our 100% baseline),
here are the percentages for each threshold:
Distribution by Method Count Thresholds:
≤ 2 interface methods: 39.7% (386 classes)
≤ 4 interface methods: 56.4% (549 classes)
≤ 8 interface methods: 81.9% (797 classes)
≤ 16 interface methods: 91.8% (893 classes)
≤ 32 interface methods: 98.5% (958 classes)


interfaces (trait), like in Rust
for interfaces, support instanceof and cast

trait Log
  info(msg i8[])
  error(msg i8[])

type Printer : Log, ...

https://aglang.org/how-the-argentum-language-makes-fast-dynamic_cast-and-method-dispatch-with-just-four-processor-instructions/

Top Class: org.apache.jackrabbit.oak.plugins.blob.BlobStoreStats
Total Interface Methods: 174 methods
Interfaces Implemented (3 interfaces):
org.apache.jackrabbit.oak.plugins.blob.ExtendedBlobStatsCollector (59 methods)
org.apache.jackrabbit.oak.spi.blob.stats.BlobStatsCollector (27 methods)
org.apache.jackrabbit.oak.spi.blob.stats.BlobStoreStatsMBean (115 methods)

Several query index implementations tied for second place with 55 methods each
(like LuceneIndex, PropertyIndex, etc.)

org.apache.jackrabbit.oak.stats.SimpleStats; Total Interfaces Implemented: 7 interfaces

Classes that implement at least one interface method (973 classes):
Average: 6.12 methods
Median: 3.00 methods

Total Interfaces: 664
Total Interface Methods: 3,490
Interface Method Statistics:
Average methods per interface: 5.26
Median methods per interface: 2.0
Range: 0 to 115 methods per interface

Top 10 Interfaces with Most Methods:
org.apache.jackrabbit.oak.spi.blob.stats.BlobStoreStatsMBean: 115 methods
org.apache.jackrabbit.oak.plugins.blob.ExtendedBlobStatsCollector: 91 methods
org.apache.lucene.search.FieldCache: 81 methods

Interface Distribution:
32.1% (213) of interfaces have exactly 1 method
13.1% (87) of interfaces have exactly 2 methods
9.9% (66) of interfaces have exactly 3 methods
7.1% (47) of interfaces have 0 methods (marker/tag interfaces)

Most Common Interface Method Counts:
0 methods: 4,882 classes (83.4%)
1 method: 267 classes (4.6%)
2 methods: 119 classes (2.0%)
3 methods: 111 classes (1.9%)
6 methods: 85 classes (1.5%)

 */
public class MetaTableTest {

    @Test
    public void constructAndLookup() {

    }

    @Test
    public void perfectHashTableSize() {
        for (int i = 8; i <= 16; i += i) {
            perfectHashTableSize(i);
        }
    }

    private static void perfectHashTableSize(int size) {
        long[] set = new long[size];
        for (int i = 0; i < size; i++) {
            set[i] = Hash.hash(i);
        }
        for (int s2 = size;; s2++) {
            outer: for (int seed = 10; seed < 10_000_000; seed++) {
                BitSet used = new BitSet(size);
                for (long x : set) {
                    long x2 = Hash.hash(x + seed);
                    int y = Hash.reduce(x2, s2);
                    if (used.get(y)) {
                        continue outer;
                    }
                    used.set(y);
                }
                // System.out.println(
                //         "size " + size + " found s2=" + s2 + " seed=" + seed + " fillRate=" + ((double) size / s2));
                return;
            }
        }
    }
}
