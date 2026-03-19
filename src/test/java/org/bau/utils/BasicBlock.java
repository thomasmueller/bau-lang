package org.bau.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class BasicBlock {
    ArrayList<BasicBlock> predecessors = new ArrayList<>();
    ArrayList<BasicBlock> successors = new ArrayList<>();

    // for variables assigned in this block:
    // key: variable name, value: version number within this basic block
    // e.g. n = 3, if we have an assignment 'n_3 = n_1 + 1' in this block
    HashMap<String, Integer> localVersions = new HashMap<>();

    // for variables that have a phi node in this block:
    // key: variable name, value: version number within this basic block
    // e.g. n = 1, if we have a phi node n_1 = { n_0, n_2 }
    HashMap<String, Integer> phiVersions = new HashMap<>();

    // for phi nodes in this block:
    // key: variable name, value: set of versions
    // e.g. n = {0, 2}, if we have a phi node n_1 = { n_0, n_2 }
    HashMap<String, HashSet<Integer>> phiSources = new HashMap<>();
}
