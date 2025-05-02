package org.bau.stdlib.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BTree implements SortedMap<String, String> {

    Page root = new Page(false);
    HashMap<String, Page> pages = new HashMap<>();
    private int maxFileSize = 10;
    private int fileCount;

    public String newFileName() {
        return "" + fileCount++;
    }

    public String get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        Page root = this.root;
        while (true) {
            if (!root.isInnerNode()) {
                int index = root.getKeyIndex(key);
                if (index >= 0) {
                    return root.getValue(index);
                }
                return null;
            }
            int index = root.getKeyIndex(key);
            if (index < 0) {
                index = -index - 2;
            }
            index++;
            String fileName = root.getChildValue(index);
            root = pages.get(fileName);
            // continue with the new file
        }
    }


    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }

    @Override
    public void remove(String key) {
        // TODO Auto-generated method stub

    }


    /**
     * Put a value.
     * To remove an entry, the value needs to be null.
     *
     * @param key the key
     * @param value the value
     */
    public void put(String key, String value) {
        if (key == null) {
            throw new NullPointerException();
        }
        put("root", key, value);
    }

    /**
     * Put a value.
     *
     * @param rootFileName
     * @param key
     * @param value (DELETED if we need to put that, or null to remove the entry)
     * @return the file name of the root (different than the passed file name, if the file was copied)
     */
    private void put(String rootFileName, String key, String value) {
        String fileName = rootFileName;
        Page file = pages.get(fileName);
        ArrayList<String> parents = new ArrayList<>();
        String k = key;
        while (true) {
            int index = file.getKeyIndex(k);
            if (!file.isInnerNode()) {
                if (index >= 0) {
                    if (value == null) {
                        file.removeRecord(index);
                    } else {
                        file.setValue(index, value);
                    }
                } else {
                    // not found
                    if (value == null) {
                        // nothing to do
                        return;
                    }
                    file.insertRecord(-index - 1, k, value);
                }
                break;
            }
            parents.add(fileName);
            if (index < 0) {
                index = -index - 2;
            }
            index++;
            fileName = file.getChildValue(index);
            file = pages.get(fileName);
            // continue with the new file
        }
        pages.put(fileName, file);
        splitOrMerge(fileName, file, parents);
    }

    private void splitOrMerge(String fileName, Page file, ArrayList<String> parents) {
        int size = file.values.size();
        if (size > maxFileSize && file.canSplit()) {
            split(fileName, file, parents);
        } else if (file.getKeys().size() == 0) {
            merge(fileName, file, parents);
        }
    }

    private void merge(String fileName, Page file, ArrayList<String> parents) {
        if (file.getValueCount() > 0) {
            return;
        }
        if (parents.isEmpty()) {
            // root: ignore
            return;
        }
        String parentFileName = parents.remove(parents.size() - 1);
        Page parentFile = pages.get(parentFileName);
        for (int i = 0; i < parentFile.getValueCount(); i++) {
            String pf = parentFile.getChildValue(i);
            if (pf.equals(fileName)) {
                if (parentFile.getValueCount() == 1) {
                    parentFile = new Page(false);
                    if (!parentFileName.startsWith("root")) {
                        String newParentFileName = "leaf" + parentFileName.substring("leaf".length());
                        pages.put(newParentFileName, parentFile);
                        updateChildFileName(parents.get(parents.size() - 1), parentFileName, newParentFileName);
                        parentFileName = newParentFileName;
                    }
                } else if (i == parentFile.getValueCount() - 1) {
                    // remove the last entry
                    parentFile.removeKey(i - 1);
                    parentFile.removeValue(i);
                } else {
                    parentFile.removeKey(i);
                    parentFile.removeValue(i);
                }
                pages.put(parentFileName, parentFile);
                merge(parentFileName, parentFile, parents);
                break;
            }
        }
    }

    private void updateChildFileName(String fileName, String oldChild, String newChild) {
        Page file = pages.get(fileName);
        for (int i = 0; i < file.getValueCount(); i++) {
            if (file.getChildValue(i).equals(oldChild)) {
                file.setValue(i, newChild);
                pages.put(fileName, file);
                return;
            }
        }
    }

    private void split(String fileName, Page file, ArrayList<String> parents) {
        List<String> keys = new ArrayList<>(file.getKeys());
        String parentFileName, newFileName1, newFileName2;
        Page parentFile, newFile1, newFile2;
        boolean isInternalNode = file.isInnerNode();
        if (parents.isEmpty()) {
            // new root
            parentFileName = fileName;
            parentFile = new Page(true);
            newFileName1 = (isInternalNode ? "inner" : "leaf") +
                    newFileName();
            parentFile.addChild(0, null, newFileName1);
        } else {
            parentFileName = parents.remove(parents.size() - 1);
            parentFile = pages.get(parentFileName);
            newFileName1 = fileName;
        }
        newFile1 = new Page(isInternalNode);
        newFileName2 = (isInternalNode ? "inner" : "leaf") +
                newFileName();
        newFile2 = new Page(isInternalNode);
        int sentinelIndex = keys.size() / 2;
        String sentinel = keys.get(sentinelIndex);
        // shorten the sentinel if possible
        String beforeSentinal = keys.get(sentinelIndex - 1);
        while (sentinel.length() > 0 && !isInternalNode) {
            // for internal nodes, there might be other keys on the left side
            // that might be shoter than the entry before the sentinel
            String oneShorter = sentinel.substring(0, sentinel.length() - 1);
            if (beforeSentinal.compareTo(oneShorter) >= 0) {
                break;
            }
            sentinel = oneShorter;
        }
        if (!isInternalNode) {
            // leaf
            for (int i = 0; i < keys.size() / 2; i++) {
                String k = keys.get(i);
                String v = file.getValue(i);
                newFile1.appendRecord(k, v);
            }
            for (int i = keys.size() / 2; i < keys.size(); i++) {
                String k = keys.get(i);
                String v = file.getValue(i);
                newFile2.appendRecord(k, v);
            }
        } else {
            // inner node
            newFile1.addChild(0, null, file.getChildValue(0));
            for (int i = 1; i <= keys.size() / 2; i++) {
                String p = keys.get(i - 1);
                newFile1.appendRecord(p, file.getChildValue(i));
            }
            newFile2.addChild(0, null, file.getChildValue(keys.size() / 2 + 1));
            for (int i = keys.size() / 2 + 2; i <= keys.size(); i++) {
                String p = keys.get(i - 1);
                newFile2.appendRecord(p, file.getChildValue(i));
            }
        }
        // insert sentinel into parent
        int index = parentFile.getKeyIndex(sentinel);
        parentFile.addChild(-index, sentinel, newFileName2);
        pages.put(newFileName1, newFile1);
        pages.put(newFileName2, newFile2);
        pages.put(parentFileName, parentFile);
        splitOrMerge(parentFileName, parentFile, parents);
    }

    public static class Page {
        private ArrayList<String> keys = new ArrayList<>();
        private ArrayList<String> values = new ArrayList<>();
        private boolean innerNode;
        // -1: beginning; 0: middle; 1: end
        private int lastSearchIndex;

        Page(boolean innerNode) {
            this.innerNode = innerNode;
        }

        public void addChild(int index, String childKey, String newChildFileName) {
            if (index > 0) {
                keys.add(index - 1, childKey);
            }
            values.add(index, newChildFileName);
        }

        public void setValue(int index, String value) {
            values.set(index, value);
        }

        public void removeRecord(int index) {
            keys.remove(index);
            values.remove(index);
        }

        public void appendRecord(String k, String v) {
            keys.add(k);
            values.add(v);
        }

        public void insertRecord(int index, String key, String value) {
            keys.add(index, key);
            values.add(index, value);
        }

        public boolean canSplit() {
            if (innerNode) {
                return keys.size() > 2;
            } else {
                return keys.size() > 1;
            }
        }

        public List<String> getKeys() {
            return keys;
        }

        public int getKeyIndex(String key) {
            int index;
            if (keys.isEmpty()) {
                return -1;
            }
            if (lastSearchIndex == Integer.MAX_VALUE) {
                if (key.compareTo(keys.get(keys.size() - 1)) > 0) {
                    index = -(keys.size() + 1);
                } else {
                    index = Collections.binarySearch(keys, key);
                }
            } else if (lastSearchIndex == Integer.MIN_VALUE) {
                if (key.compareTo(keys.get(0)) < 0) {
                    index = -1;
                } else {
                    index = Collections.binarySearch(keys, key);
                }
            } else if (lastSearchIndex == 0 || keys.size() < 8) {
                index = Collections.binarySearch(keys, key);
            } else {
                index = binarySearchWithGuess(keys, key, lastSearchIndex);
                lastSearchIndex = Math.abs(index);
            }
            if (index == -(keys.size() + 1)) {
                lastSearchIndex = Integer.MAX_VALUE;
            } else if (index == -1) {
                lastSearchIndex = Integer.MIN_VALUE;
            } else {
                lastSearchIndex = Math.abs(index);
            }
            return index;
        }

        private static int binarySearchWithGuess(ArrayList<String> list, String key, int guess) {
            int x = guess;
            int low = 0;
            int high = list.size() - 1;
            int step = 2;
            while (low <= high) {
                x = Math.max(x, low);
                x = Math.min(x, high);
                String midVal = list.get(x);
                int comp = key.compareTo(midVal);
                if (comp > 0) {
                    low = x + 1;
                    x = low + step;
                } else if (comp < 0) {
                    high = x - 1;
                    x = high - step;
                } else {
                    return x;
                }
                if (high - low < 8 || step >= 256) {
                    int result = binarySearch(list, key, low, high);
                    return result;
                }
                step *= 8;
            }
            return -(low + 1);
        }

        private static int binarySearch(ArrayList<String> list, String key, int low, int high) {
            int x = (low + high) >>> 1;
            while (low <= high) {
                String midVal = list.get(x);
                int comp = key.compareTo(midVal);
                if (comp > 0) {
                    low = x + 1;
                } else if (comp < 0) {
                    high = x - 1;
                } else {
                    return x;
                }
                x = (low + high) >>> 1;
            }
            return -(low + 1);
        }

        public String getValue(int index) {
            return values.get(index);
        }

        public String getChildValue(int index) {
            return (String) values.get(index);
        }

        public String getNextKey(String largerThan) {
            int index;
            if (largerThan == null) {
                index = 0;
            } else {
                index = getKeyIndex(largerThan);
                if (index < 0) {
                    index = -index - 1;
                } else {
                    index++;
                }
            }
            if (index < 0 || index >= keys.size()) {
                return null;
            }
            return keys.get(index);
        }

        public void removeKey(int index) {
            keys.remove(index);
        }

        public void removeValue(int index) {
            values.remove(index);
        }

        public boolean isInnerNode() {
            return innerNode;
        }

        public int getValueCount() {
            return values.size();
        }

        public String getKey(int index) {
            return keys.get(index);
        }

    }
}
