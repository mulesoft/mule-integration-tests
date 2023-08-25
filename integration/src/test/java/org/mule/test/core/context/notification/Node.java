/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.Notification.Action;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We test notifications by defining a "tree" of expected responses (this is needed because the system is multithreaded and only
 * some ordering is guaranteed serial; other actions happen in parallel) Each node can test for a notification and then has a set
 * of parallel nodes, which describe which notifications happen next in any order. Finally, after all parallel nodes are matched,
 * a node has a set of serial nodes, which are matched in order.
 *
 * <p>
 * Note that nested nodes may themselves have structure and that empty nodes are available, which can help group dependencies.
 *
 * <p>
 * More exactly, we specify a tree and a traversal - the traversal is hardcoded below, and implicit in the instructions above.
 */
public class Node implements RestrictedNode {

  // enumeration describing result of checking at this node
  public static final int SUCCESS = 0;
  public static final int FAILURE = 1;
  public static final int EMPTY = 2;

  // the data for this node
  private Class clazz = null;
  private Action action;
  private String id;
  private boolean isIdDefined = false; // allow null IDs to be specified
  private boolean nodeOk = false;

  protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());

  // any of these can run after this
  private Set parallel = new HashSet();
  // only once the parallel are done, this runs
  private LinkedList serial = new LinkedList();

  public Node(Class clazz, Action action, String id) {
    this(clazz, action);
    this.id = id;
    isIdDefined = true;
  }

  public Node(Class clazz, Action action) {
    this.clazz = clazz;
    this.action = action;
  }

  public Node() {
    nodeOk = true;
  }

  public Node parallel(RestrictedNode node) {
    parallel.add(node);
    return this;
  }

  @Override
  public RestrictedNode serial(RestrictedNode node) {
    serial.addLast(node);
    return this;
  }

  /**
   * @param notification
   * @return whether the notification was matched or not (for this node or any child)
   */
  @Override
  public int match(AbstractServerNotification notification) {
    // if we need to check ourselves, just do that
    if (!nodeOk) {
      if (testLocal(notification)) {
        nodeOk = true;
        return SUCCESS;
      } else {
        return FAILURE;
      }
    }

    // otherwise, if we have parallel children, try them
    if (parallel.size() > 0) {
      for (Iterator children = parallel.iterator(); children.hasNext();) {
        Node child = (Node) children.next();
        switch (child.match(notification)) {
          case SUCCESS:
            return SUCCESS;
          case EMPTY: // the node was empty, clean out
            children.remove();
            break;
          case FAILURE:
            break;
          default:
            throw new IllegalStateException("Bad return from child");
        }
      }
    }

    // if we've still got parallel children, we failed
    if (parallel.size() > 0) {
      return FAILURE;
    }

    // otherwise, serial children
    if (serial.size() > 0) {
      for (Iterator children = serial.iterator(); children.hasNext();) {
        Node child = (Node) children.next();
        switch (child.match(notification)) {
          case SUCCESS:
            return SUCCESS;
          case EMPTY: // the node was empty, clean out
            children.remove();
            break;
          case FAILURE:
            return FAILURE; // note this is different to parallel case
          default:
            throw new IllegalStateException("Bad return from child");
        }
      }

    }

    if (serial.size() > 0) {
      return FAILURE;
    } else {
      return EMPTY;
    }
  }

  private boolean testLocal(AbstractServerNotification notification) {
    return clazz.equals(notification.getClass()) && action.equals(notification.getAction())
        && (!isIdDefined || (null == id && null == notification.getResourceIdentifier())
            || (null != id && id.equals(notification.getResourceIdentifier())));
  }

  @Override
  public boolean contains(Class clazz, Action action) {
    if (null != this.clazz && this.clazz.equals(clazz) && this.action.equals(action)) {
      return true;
    }
    for (Iterator children = parallel.iterator(); children.hasNext();) {
      if (((RestrictedNode) children.next()).contains(clazz, action)) {
        return true;
      }
    }
    for (Iterator children = serial.iterator(); children.hasNext();) {
      if (((RestrictedNode) children.next()).contains(clazz, action)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public RestrictedNode getAnyRemaining() {
    if (!nodeOk) {
      return this;
    }
    for (Iterator children = parallel.iterator(); children.hasNext();) {
      RestrictedNode any = ((RestrictedNode) children.next()).getAnyRemaining();
      if (null != any) {
        return any;
      }
    }
    for (Iterator children = serial.iterator(); children.hasNext();) {
      RestrictedNode any = ((RestrictedNode) children.next()).getAnyRemaining();
      if (null != any) {
        return any;
      }
    }
    return null;
  }

  @Override
  public boolean isExhausted() {
    return null == getAnyRemaining();
  }

  @Override
  public Class getNotificationClass() {
    return clazz;
  }

  @Override
  public String toString() {
    return clazz + ": " + action + (isIdDefined ? ": " + id : "");
  }

}
