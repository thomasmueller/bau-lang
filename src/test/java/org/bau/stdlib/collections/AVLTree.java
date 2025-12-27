package org.bau.stdlib.collections;

public class AVLTree<K extends Comparable<K>, V> implements SortedMap<K, V> {

    private TreeNode<K, V> root;
    private int size;

    private int compare(TreeNode<K, V> node, K key) {
        return key.compareTo(node.key);
    }

    public V get(K key) {
        TreeNode<K, V> n = findFirstNode(key);
        if (n != null && compare(n, key) == 0) {
            return n.value;
        }
        return null;
    }

    public void put(K key, V value) {
        TreeNode<K, V> n = root, x = n;
        boolean isLeft = true;
        while (n != null) {
            int compare = compare(n, key);
            if (compare == 0) {
                n.value = value;
                return;
            }
            isLeft = compare < 0;
            x = n;
            n = child(x, isLeft);
        }
        TreeNode<K, V> i = new TreeNode<>(key, value);
        if (x == null) {
            root = i;
        } else {
            set(x, isLeft, i);
            balance(x, isLeft);
        }
        size++;
    }

    private TreeNode<K, V> child(TreeNode<K, V> x, boolean isLeft) {
        return isLeft ? x.left : x.right;
    }

    private void replace(TreeNode<K, V> x, TreeNode<K, V> n) {
        if (x == root) {
            root = n;
            if (n != null) {
                n.parent = null;
            }
        } else {
            set(x.parent, x.isFromLeft(), n);
        }
    }

    private void set(TreeNode<K, V> parent, boolean left, TreeNode<K, V> n) {
        if (left) {
            parent.left = n;
        } else {
            parent.right = n;
        }
        if (n != null) {
            n.parent = parent;
        }
    }

    private TreeNode<K, V> findFirstNode(K key) {
        TreeNode<K, V> x = root, result = x;
        while (x != null) {
            result = x;
            int compare = -compare(x, key);
            if (compare == 0) {
                return x;
            } else if (compare > 0) {
                x = x.left;
            } else {
                x = x.right;
            }
        }
        return result;
    }

    public void remove(K key) {
        TreeNode<K, V> x = findFirstNode(key);
        if (x == null || compare(x, key) != 0) {
            return;
        }
        TreeNode<K, V> n;
        if (x.left == null) {
            n = x.right;
        } else if (x.right == null) {
            n = x.left;
        } else {
            TreeNode<K, V> d = x;
            x = x.left;
            for (TreeNode<K, V> temp = x; (temp = temp.right) != null;) {
                x = temp;
            }
            // x will be replaced with n later
            n = x.left;
            // swap d and x
            int b = x.balance;
            x.balance = d.balance;
            d.balance = b;
            // set x.parent
            TreeNode<K, V> xp = x.parent;
            TreeNode<K, V> dp = d.parent;
            if (d == root) {
                root = x;
            }
            x.parent = dp;
            if (dp != null) {
                if (dp.right == d) {
                    dp.right = x;
                } else {
                    dp.left = x;
                }
            }
            // TODO index / tree: link d.r = x(p?).r directly
            if (xp == d) {
                d.parent = x;
                if (d.left == x) {
                    x.left = d;
                    x.right = d.right;
                } else {
                    x.right = d;
                    x.left = d.left;
                }
            } else {
                d.parent = xp;
                xp.right = d;
                x.right = d.right;
                x.left = d.left;
            }
            if (x.right == null) {
                throw new IllegalStateException("corrupt");
            }
            x.right.parent = x;
            x.left.parent = x;
            // set d.left, d.right
            d.left = n;
            if (n != null) {
                n.parent = d;
            }
            d.right = null;
            x = d;
        }
        size--;
        boolean isLeft = x.isFromLeft();
        replace(x, n);
        n = x.parent;
        while (n != null) {
            x = n;
            int sign = isLeft ? 1 : -1;
            switch (x.balance * sign) {
            case -1:
                x.balance = 0;
                break;
            case 0:
                x.balance = sign;
                return;
            case 1:
                TreeNode<K, V> r = child(x, !isLeft);
                int b = r.balance;
                if (b * sign >= 0) {
                    replace(x, r);
                    set(x, !isLeft, child(r, isLeft));
                    set(r, isLeft, x);
                    if (b == 0) {
                        x.balance = sign;
                        r.balance = -sign;
                        return;
                    }
                    x.balance = 0;
                    r.balance = 0;
                    x = r;
                } else {
                    TreeNode<K, V> l = child(r, isLeft);
                    replace(x, l);
                    b = l.balance;
                    set(r, isLeft, child(l, !isLeft));
                    set(l, !isLeft, r);
                    set(x, !isLeft, child(l, isLeft));
                    set(l, isLeft, x);
                    x.balance = (b == sign) ? -sign : 0;
                    r.balance = (b == -sign) ? sign : 0;
                    l.balance = 0;
                    x = l;
                }
                break;
            default:
                throw new IllegalStateException("b: " + x.balance * sign);
            }
            isLeft = x.isFromLeft();
            n = x.parent;
        }
    }

    private void balance(TreeNode<K, V> x, boolean isLeft) {
        while (true) {
            int sign = isLeft ? 1 : -1;
            switch (x.balance * sign) {
            case 1:
                x.balance = 0;
                return;
            case 0:
                x.balance = -sign;
                break;
            case -1:
                TreeNode<K, V> l = child(x, isLeft);
                if (l.balance == -sign) {
                    replace(x, l);
                    set(x, isLeft, child(l, !isLeft));
                    set(l, !isLeft, x);
                    x.balance = 0;
                    l.balance = 0;
                } else {
                    TreeNode<K, V> r = child(l, !isLeft);
                    replace(x, r);
                    set(l, !isLeft, child(r, isLeft));
                    set(r, isLeft, l);
                    set(x, isLeft, child(r, !isLeft));
                    set(r, !isLeft, x);
                    int rb = r.balance;
                    x.balance = (rb == -sign) ? sign : 0;
                    l.balance = (rb == sign) ? -sign : 0;
                    r.balance = 0;
                }
                return;
            default:
                throw new IllegalStateException("b:" + x.balance * sign);
            }
            if (x == root) {
                return;
            }
            isLeft = x.isFromLeft();
            x = x.parent;
        }
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    @Override
    public int size() {
        return size;
    }

    static class TreeNode<K, V> {
        K key;
        V value;
        int balance;
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        TreeNode<K, V> parent;

        TreeNode(K key, V value) {
            this.key = key;
            this.value = value;
        }

        boolean isFromLeft() {
            return parent == null || parent.left == this;
        }

    }

}
