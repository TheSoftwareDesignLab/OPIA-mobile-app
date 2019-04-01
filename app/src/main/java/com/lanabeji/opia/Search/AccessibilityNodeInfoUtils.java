package com.lanabeji.opia.Search;

/**
 * Created by lanabeji on 30/03/19.
 */

import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import android.support.v4.view.accessibility.AccessibilityWindowInfoCompat;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Provides a series of utilities for interacting with AccessibilityNodeInfo objects. NOTE: This
 * class only recycles unused nodes that were collected internally. Any node passed into or returned
 * from a public method is retained and TalkBack should recycle it when appropriate.
 */
public class AccessibilityNodeInfoUtils {


    /**
     * A wrapper over AccessibilityNodeInfoCompat constructor, so that we can add any desired error
     * checking and memory management.
     *
     * @param nodeInfo The AccessibilityNodeInfo which will be wrapped. The caller retains the
     *     responsibility to recycle nodeInfo.
     * @return Encapsulating AccessibilityNodeInfoCompat, or null if input is invalid.
     */
    public static @Nullable AccessibilityNodeInfoCompat toCompat(
            @Nullable AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return null;
        }
        return new AccessibilityNodeInfoCompat(nodeInfo);
    }

    private static final int SYSTEM_ACTION_MAX = 0x01FFFFFF;
    private static final Pattern RESOURCE_NAME_SPLIT_PATTERN = Pattern.compile(":id/");

    private AccessibilityNodeInfoUtils() {
        // This class is not instantiable.
    }

    /**
     * Gets the text of a <code>node</code> by returning the content description (if available) or by
     * returning the text.
     *
     * @param node The node.
     * @return The node text.
     */
    public static CharSequence getNodeText(AccessibilityNodeInfoCompat node) {
        if (node == null) {
            return null;
        }

        // Prefer content description over text.
        // TODO: Why are we checking the trimmed length?
        final CharSequence contentDescription = node.getContentDescription();
        if (!TextUtils.isEmpty(contentDescription)
                && (TextUtils.getTrimmedLength(contentDescription) > 0)) {
            return contentDescription;
        }

        final CharSequence text = node.getText();
        if (!TextUtils.isEmpty(text) && (TextUtils.getTrimmedLength(text) > 0)) {
            return text;
        }

        return null;
    }

    /**
     * Gets the textual representation of the view ID that can be used when no custom label is
     * available. For better readability/listenability, the "_" characters are replaced with spaces.
     *
     * @param node The node
     * @return Readable text of the view Id
     */
    public static String getViewIdText(AccessibilityNodeInfoCompat node) {
        if (node == null) {
            return null;
        }

        String resourceName = node.getViewIdResourceName();
        if (resourceName == null) {
            return null;
        }

        String[] parsedResourceName = RESOURCE_NAME_SPLIT_PATTERN.split(resourceName, 2);
        if (parsedResourceName.length != 2
                || TextUtils.isEmpty(parsedResourceName[0])
                || TextUtils.isEmpty(parsedResourceName[1])) {
            return null;
        }

        return parsedResourceName[1].replace('_', ' '); // readable View ID text
    }

    public static List<AccessibilityActionCompat> getCustomActions(AccessibilityNodeInfoCompat node) {
        List<AccessibilityActionCompat> customActions = new ArrayList<>();
        for (AccessibilityActionCompat action : node.getActionList()) {
            if (isCustomAction(action)) {
                // We don't use custom actions that doesn't have a label
                if (!TextUtils.isEmpty(action.getLabel())) {
                    customActions.add(action);
                }
            }
        }

        return customActions;
    }

    public static boolean isCustomAction(AccessibilityActionCompat action) {
        return action.getId() > SYSTEM_ACTION_MAX;
    }

    /** Returns the root node of the tree containing {@code node}. */
    public static AccessibilityNodeInfoCompat getRoot(AccessibilityNodeInfoCompat node) {
        if (node == null) {
            return null;
        }

        Set<AccessibilityNodeInfoCompat> visitedNodes = new HashSet<>();
        AccessibilityNodeInfoCompat current = null;
        AccessibilityNodeInfoCompat parent = AccessibilityNodeInfoCompat.obtain(node);

        try {
            do {
                if (current != null) {
                    if (visitedNodes.contains(current)) {
                        current.recycle();
                        parent.recycle();
                        return null;
                    }
                    visitedNodes.add(current);
                }

                current = parent;
                parent = current.getParent();
            } while (parent != null);
        } finally {
            recycleNodes(visitedNodes);
        }

        return current;
    }

    private static boolean hasVisibleChildren(AccessibilityNodeInfoCompat node) {
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            AccessibilityNodeInfoCompat child = node.getChild(i);
            if (child != null) {
                try {
                    if (child.isVisibleToUser()) {
                        return true;
                    }
                } finally {
                    child.recycle();
                }
            }
        }

        return false;
    }

    public static int countVisibleChildren(AccessibilityNodeInfoCompat node) {
        if (node == null) {
            return 0;
        }
        int childCount = node.getChildCount();
        int childVisibleCount = 0;
        for (int i = 0; i < childCount; ++i) {
            AccessibilityNodeInfoCompat child = node.getChild(i);
            if (child != null) {
                try {
                    if (child.isVisibleToUser()) {
                        ++childVisibleCount;
                    }
                } finally {
                    child.recycle();
                }
            }
        }
        return childVisibleCount;
    }

    /**
     * Returns whether a node is actionable. That is, the node supports one of the following actions:
     *
     * <ul>
     *   <li>{@link AccessibilityNodeInfoCompat#isClickable()}
     *   <li>{@link AccessibilityNodeInfoCompat#isFocusable()}
     *   <li>{@link AccessibilityNodeInfoCompat#isLongClickable()}
     * </ul>
     *
     * This parities the system method View#isActionableForAccessibility(), which was added in
     * JellyBean.
     *
     * @param node The node to examine.
     * @return {@code true} if node is actionable.
     */
    public static boolean isActionableForAccessibility(AccessibilityNodeInfoCompat node) {
        if (node == null) {
            return false;
        }

        // Nodes that are clickable are always actionable.
        if (isClickable(node) || isLongClickable(node)) {
            return true;
        }

        if (node.isFocusable()) {
            return true;
        }

        return supportsAnyAction(
                node,
                AccessibilityNodeInfoCompat.ACTION_FOCUS,
                AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT,
                AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT);
    }

    /**
     * Returns whether a node is clickable. That is, the node supports at least one of the following:
     *
     * <ul>
     *   <li>{@link AccessibilityNodeInfoCompat#isClickable()}
     *   <li>{@link AccessibilityNodeInfoCompat#ACTION_CLICK}
     * </ul>
     *
     * @param node The node to examine.
     * @return {@code true} if node is clickable.
     */
    public static boolean isClickable(AccessibilityNodeInfoCompat node) {
        return node != null
                && (node.isClickable()
                || supportsAnyAction(node, AccessibilityNodeInfoCompat.ACTION_CLICK));
    }

    /**
     * Returns whether a node is long clickable. That is, the node supports at least one of the
     * following:
     *
     * <ul>
     *   <li>{@link AccessibilityNodeInfoCompat#isLongClickable()}
     *   <li>{@link AccessibilityNodeInfoCompat#ACTION_LONG_CLICK}
     * </ul>
     *
     * @param node The node to examine.
     * @return {@code true} if node is long clickable.
     */
    public static boolean isLongClickable(AccessibilityNodeInfoCompat node) {
        return node != null
                && (node.isLongClickable()
                || supportsAnyAction(node, AccessibilityNodeInfoCompat.ACTION_LONG_CLICK));
    }

    /**
     * Check whether a given node has a scrollable ancestor.
     *
     * @param node The node to examine.
     * @return {@code true} if one of the node's ancestors is scrollable.
     */
    public static boolean hasMatchingAncestor(
            AccessibilityNodeInfoCompat node, Filter<AccessibilityNodeInfoCompat> filter) {
        if (node == null) {
            return false;
        }

        final AccessibilityNodeInfoCompat result = getMatchingAncestor(node, filter);
        if (result == null) {
            return false;
        }

        result.recycle();
        return true;
    }


    /** Check whether a given node has any descendant matching a given filter. */
    public static boolean hasMatchingDescendant(
            AccessibilityNodeInfoCompat node, Filter<AccessibilityNodeInfoCompat> filter) {
        if (node == null) {
            return false;
        }

        final AccessibilityNodeInfoCompat result = getMatchingDescendant(node, filter);
        if (result == null) {
            return false;
        }

        result.recycle();
        return true;
    }

    /**
     * Returns the {@code node} if it matches the {@code filter}, or the first matching descendant.
     * Returns {@code null} if no nodes match.
     */
    public static AccessibilityNodeInfoCompat getSelfOrMatchingDescendant(
            AccessibilityNodeInfoCompat node, Filter<AccessibilityNodeInfoCompat> filter) {
        if (node == null) {
            return null;
        }

        if (filter.accept(node)) {
            return AccessibilityNodeInfoCompat.obtain(node);
        }

        return getMatchingDescendant(node, filter);
    }


    /**
     * Returns the first ancestor of {@code node} that matches the {@code filter}. Returns {@code
     * null} if no nodes match.
     */
    public static AccessibilityNodeInfoCompat getMatchingAncestor(
            AccessibilityNodeInfoCompat node, Filter<AccessibilityNodeInfoCompat> filter) {
        return getMatchingAncestor(node, null, filter);
    }

    /**
     * Returns the first ancestor of {@code node} that matches the {@code filter}, terminating the
     * search once it reaches {@code end}. The search is exclusive of both {@code node} and {@code
     * end}. Returns {@code null} if no nodes match.
     */
    private static AccessibilityNodeInfoCompat getMatchingAncestor(
            AccessibilityNodeInfoCompat node,
            AccessibilityNodeInfoCompat end,
            Filter<AccessibilityNodeInfoCompat> filter) {
        if (node == null) {
            return null;
        }

        final HashSet<AccessibilityNodeInfoCompat> ancestors = new HashSet<>();

        try {
            ancestors.add(AccessibilityNodeInfoCompat.obtain(node));
            node = node.getParent();

            while (node != null) {
                if (!ancestors.add(node)) {
                    // Already seen this node, so abort!
                    node.recycle();
                    return null;
                }

                if (end != null && node.equals(end)) {
                    // Reached the end node, so abort!
                    // Don't recycle the node here, it was added to ancestors and will be recycled.
                    return null;
                }

                if (filter.accept(node)) {
                    // Send a copy since node gets recycled.
                    return AccessibilityNodeInfoCompat.obtain(node);
                }

                node = node.getParent();
            }
        } finally {
            recycleNodes(ancestors);
        }

        return null;
    }

    /**
     * Returns the number of ancestors matching the given filter. Does not include the current node in
     * the count, even if it matches the filter. If there is a cycle in the ancestor hierarchy, then
     * this method will return 0.
     */
    public static int countMatchingAncestors(
            AccessibilityNodeInfoCompat node, Filter<AccessibilityNodeInfoCompat> filter) {
        if (node == null) {
            return 0;
        }

        final HashSet<AccessibilityNodeInfoCompat> ancestors = new HashSet<>();
        int matchingAncestors = 0;

        try {
            ancestors.add(AccessibilityNodeInfoCompat.obtain(node));
            node = node.getParent();

            while (node != null) {
                if (!ancestors.add(node)) {
                    // Already seen this node, so abort!
                    node.recycle();
                    return 0;
                }

                if (filter.accept(node)) {
                    matchingAncestors++;
                }

                node = node.getParent();
            }
        } finally {
            recycleNodes(ancestors);
        }

        return matchingAncestors;
    }

    /**
     * Returns the first child (by depth-first search) of {@code node} that matches the {@code
     * filter}. Returns {@code null} if no nodes match. The caller is responsible for recycling all
     * nodes in {@code visitedNodes} and the node returned by this method, if non-{@code null}.
     */
    private static AccessibilityNodeInfoCompat getMatchingDescendant(
            AccessibilityNodeInfoCompat node,
            Filter<AccessibilityNodeInfoCompat> filter,
            HashSet<AccessibilityNodeInfoCompat> visitedNodes) {
        if (node == null) {
            return null;
        }

        if (visitedNodes.contains(node)) {
            return null;
        } else {
            visitedNodes.add(AccessibilityNodeInfoCompat.obtain(node));
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            AccessibilityNodeInfoCompat child = node.getChild(i);

            if (child == null) {
                continue;
            }

            if (filter.accept(child)) {
                return child; // child was already obtained by node.getChild().
            }

            try {
                AccessibilityNodeInfoCompat childMatch = getMatchingDescendant(child, filter, visitedNodes);
                if (childMatch != null) {
                    return childMatch;
                }
            } finally {
                child.recycle();
            }
        }

        return null;
    }

    private static AccessibilityNodeInfoCompat getMatchingDescendant(
            AccessibilityNodeInfoCompat node, Filter<AccessibilityNodeInfoCompat> filter) {
        final HashSet<AccessibilityNodeInfoCompat> visitedNodes = new HashSet<>();
        try {
            return getMatchingDescendant(node, filter, visitedNodes);
        } finally {
            recycleNodes(visitedNodes);
        }
    }

    /**
     * Check whether a given node is scrollable.
     *
     * @param node The node to examine.
     * @return {@code true} if the node is scrollable.
     */
    public static boolean isScrollable(AccessibilityNodeInfoCompat node) {
        return node.isScrollable()
                || supportsAnyAction(
                node,
                AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD,
                AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
    }

    /**
     * Returns whether the specified node has text. For the purposes of this check, any node with a
     * CollectionInfo is considered to not have text since its text and content description are used
     * only for collection transitions.
     *
     * @param node The node to check.
     * @return {@code true} if the node has text.
     */
    private static boolean hasText(AccessibilityNodeInfoCompat node) {
        return node != null
                && node.getCollectionInfo() == null
                && (!TextUtils.isEmpty(node.getText()) || !TextUtils.isEmpty(node.getContentDescription()));
    }

    public static boolean hasAncestor(
            AccessibilityNodeInfoCompat node, final AccessibilityNodeInfoCompat targetAncestor) {
        if (node == null || targetAncestor == null) {
            return false;
        }

        Filter<AccessibilityNodeInfoCompat> filter =
                new Filter<AccessibilityNodeInfoCompat>() {
                    @Override
                    public boolean accept(AccessibilityNodeInfoCompat node) {
                        return targetAncestor.equals(node);
                    }
                };

        AccessibilityNodeInfoCompat foundAncestor = getMatchingAncestor(node, filter);
        if (foundAncestor != null) {
            foundAncestor.recycle();
            return true;
        }

        return false;
    }

    public static boolean hasDescendant(
            AccessibilityNodeInfoCompat node, final AccessibilityNodeInfoCompat targetDescendant) {
        if (node == null || targetDescendant == null) {
            return false;
        }

        Filter<AccessibilityNodeInfoCompat> filter =
                new Filter<AccessibilityNodeInfoCompat>() {
                    @Override
                    public boolean accept(AccessibilityNodeInfoCompat node) {
                        return targetDescendant.equals(node);
                    }
                };

        AccessibilityNodeInfoCompat foundAncestor = getMatchingDescendant(node, filter);
        if (foundAncestor != null) {
            foundAncestor.recycle();
            return true;
        }

        return false;
    }

    /**
     * Recycles the given nodes.
     *
     * @param nodes The nodes to recycle.
     */
    public static void recycleNodes(Collection<AccessibilityNodeInfoCompat> nodes) {
        if (nodes == null) {
            return;
        }

        for (AccessibilityNodeInfoCompat node : nodes) {
            if (node != null) {
                node.recycle();
            }
        }

        nodes.clear();
    }

    /**
     * Recycles the given nodes.
     *
     * @param nodes The nodes to recycle.
     */
    public static void recycleNodes(AccessibilityNodeInfo... nodes) {
        if (nodes == null) {
            return;
        }

        for (AccessibilityNodeInfo node : nodes) {
            if (node != null) {
                node.recycle();
            }
        }
    }

    /**
     * Recycles the given nodes.
     *
     * @param nodes The nodes to recycle.
     */
    public static void recycleNodes(AccessibilityNodeInfoCompat... nodes) {
        if (nodes == null) {
            return;
        }

        for (AccessibilityNodeInfoCompat node : nodes) {
            if (node != null) {
                node.recycle();
            }
        }
    }

    /**
     * Returns {@code true} if the node supports at least one of the specified actions. To check
     * whether a node supports multiple actions, combine them using the {@code |} (logical OR)
     * operator.
     *
     * <p>Note: this method will check against the getActions() method of AccessibilityNodeInfo, which
     * will not contain information for actions introduced in API level 21 or later.
     *
     * @param node The node to check.
     * @param actions The actions to check.
     * @return {@code true} if at least one action is supported.
     */
    public static boolean supportsAnyAction(AccessibilityNodeInfoCompat node, int... actions) {
        if (node != null) {
            final int supportedActions = node.getActions();

            for (int action : actions) {
                if ((supportedActions & action) == action) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns {@code true} if the node supports the specified action. This method supports actions
     * introduced in API level 21 and later. However, it does not support bitmasks.
     */
    public static boolean supportsAction(AccessibilityNodeInfoCompat node, int action) {
        // New actions in >= API 21 won't appear in getActions() but in getActionList().
        // On Lollipop+ devices, pre-API 21 actions will also appear in getActionList().
        List<AccessibilityActionCompat> actions = node.getActionList();
        int size = actions.size();
        for (int i = 0; i < size; ++i) {
            AccessibilityActionCompat actionCompat = actions.get(i);
            if (actionCompat.getId() == action) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the result of applying a filter using breadth-first traversal.
     *
     * @param node The root node to traverse from.
     * @param filter The filter to satisfy.
     * @return The first node reached via BFS traversal that satisfies the filter.
     */
    public static AccessibilityNodeInfoCompat searchFromBfs(
            AccessibilityNodeInfoCompat node, Filter<AccessibilityNodeInfoCompat> filter) {
        if (node == null) {
            return null;
        }

        final LinkedList<AccessibilityNodeInfoCompat> queue = new LinkedList<>();
        Set<AccessibilityNodeInfoCompat> visitedNodes = new HashSet<>();

        queue.add(AccessibilityNodeInfoCompat.obtain(node));

        try {
            while (!queue.isEmpty()) {
                final AccessibilityNodeInfoCompat item = queue.removeFirst();
                visitedNodes.add(item);

                if (filter.accept(item)) {
                    return item;
                }

                final int childCount = item.getChildCount();

                for (int i = 0; i < childCount; i++) {
                    final AccessibilityNodeInfoCompat child = item.getChild(i);

                    if (child != null && !visitedNodes.contains(child)) {
                        queue.addLast(child);
                    }
                }
                item.recycle();
            }
        } finally {
            while (!queue.isEmpty()) {
                queue.removeFirst().recycle();
            }
        }

        return null;
    }

    /** Safely obtains a copy of node. Caller must recycle returned node info. */
    public static AccessibilityNodeInfoCompat obtain(AccessibilityNodeInfoCompat node) {
        return (node == null) ? null : AccessibilityNodeInfoCompat.obtain(node);
    }

    /** Safely obtains a copy of node. Caller must recycle returned node info. */
    public static AccessibilityNodeInfo obtain(AccessibilityNodeInfo node) {
        return (node == null) ? null : AccessibilityNodeInfo.obtain(node);
    }

    /**
     * Returns a fresh copy of {@code node} with properties that are less likely to be stale. Returns
     * {@code null} if the node can't be found anymore.
     */
    public static AccessibilityNodeInfoCompat refreshNode(AccessibilityNodeInfoCompat node) {
        if (node == null) {
            return null;
        }

        AccessibilityNodeInfoCompat nodeCopy = AccessibilityNodeInfoCompat.obtain(node);
        if (nodeCopy.refresh()) {
            return nodeCopy;
        } else {
            nodeCopy.recycle();
            return null;
        }
    }

    public static String actionToString(int action) {
        switch (action) {
            case AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS:
                return "ACTION_ACCESSIBILITY_FOCUS";
            case AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                return "ACTION_CLEAR_ACCESSIBILITY_FOCUS";
            case AccessibilityNodeInfoCompat.ACTION_CLEAR_FOCUS:
                return "ACTION_CLEAR_FOCUS";
            case AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION:
                return "ACTION_CLEAR_SELECTION";
            case AccessibilityNodeInfoCompat.ACTION_CLICK:
                return "ACTION_CLICK";
            case AccessibilityNodeInfoCompat.ACTION_COLLAPSE:
                return "ACTION_COLLAPSE";
            case AccessibilityNodeInfoCompat.ACTION_COPY:
                return "ACTION_COPY";
            case AccessibilityNodeInfoCompat.ACTION_CUT:
                return "ACTION_CUT";
            case AccessibilityNodeInfoCompat.ACTION_DISMISS:
                return "ACTION_DISMISS";
            case AccessibilityNodeInfoCompat.ACTION_EXPAND:
                return "ACTION_EXPAND";
            case AccessibilityNodeInfoCompat.ACTION_FOCUS:
                return "ACTION_FOCUS";
            case AccessibilityNodeInfoCompat.ACTION_LONG_CLICK:
                return "ACTION_LONG_CLICK";
            case AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY:
                return "ACTION_NEXT_AT_MOVEMENT_GRANULARITY";
            case AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT:
                return "ACTION_NEXT_HTML_ELEMENT";
            case AccessibilityNodeInfoCompat.ACTION_PASTE:
                return "ACTION_PASTE";
            case AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY:
                return "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY";
            case AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT:
                return "ACTION_PREVIOUS_HTML_ELEMENT";
            case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD:
                return "ACTION_SCROLL_BACKWARD";
            case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD:
                return "ACTION_SCROLL_FORWARD";
            case AccessibilityNodeInfoCompat.ACTION_SELECT:
                return "ACTION_SELECT";
            case AccessibilityNodeInfoCompat.ACTION_SET_SELECTION:
                return "ACTION_SET_SELECTION";
            case AccessibilityNodeInfoCompat.ACTION_SET_TEXT:
                return "ACTION_SET_TEXT";
            default:
                return "(unhandled)";
        }
    }
}
