/*
 * This file is part of JBotSim.
 * 
 *    JBotSim is free software: you can redistribute it and/or modify it
 *    under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *  
 *    Authors:
 *    Arnaud Casteigts		<casteig@site.uottawa.ca>
 */
package jbotsim;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Vector;

import jbotsim.Link.Mode;
import jbotsim.Link.Type;
import jbotsim.event.ConnectivityListener;
import jbotsim.event.MovementListener;
import jbotsim.event.TopologyListener;

public class Topology extends _Properties{
    Vector<ConnectivityListener> cxUndirectedListeners=new Vector<ConnectivityListener>();
    Vector<ConnectivityListener> cxDirectedListeners=new Vector<ConnectivityListener>();
    Vector<TopologyListener> topologyListeners=new Vector<TopologyListener>();
    Vector<MovementListener> movementListeners=new Vector<MovementListener>();
    Message.MessageEngine messageEngine=new Message.MessageEngine(this);
    Vector<Node> nodes=new Vector<Node>();
    Vector<Link> arcs=new Vector<Link>();
    Vector<Link> edges=new Vector<Link>();
    
    /**
     * Removes all the nodes (and links) of this topology.
     */
    public void clear(){
        for (Node n : new Vector<Node>(nodes))
            removeNode(n);
    }
    /**
     * Adds the specified node to this topology. The location of the node
     * in the topology will be its current inherent location (or <tt>(0,0)</tt>
     * if no location was prealably given to it).
     * @param n The node to be added.
     */
    public void addNode(Node n){
        this.addNode(n.getX(), n.getY(), n);
    }
    /**
     * Adds a new node to this topology at the specified location. The node
     * will be created using default settings FIXME
     * @param x The abscissa of the location.
     * @param y The ordinate of the location.
     */
    public void addNode(double x, double y){
        this.addNode(x, y, Node.newInstanceOfModel("default"));
    }
    /**
     * Adds the specified node to this topology at the specified location.
     * @param x The abscissa of the location.
     * @param y The ordinate of the location.
     * @param n The node to be added.
     */
    public void addNode(double x, double y, Node n){
        if (x == -1)
        	x = (new Random()).nextDouble() * 600;
        if (y == -1)
        	y = (new Random()).nextDouble() * 400;
        n.setLocation(x, y);
        this.nodes.add(n);
        n.topo=this;
        this.updateWirelessLinksFor(n);
        this.notifyNodeAdded(n);
    }
    /**
     * Removes the specified node from this topology. All adjacent links will
     * be automatically removed.
     * @param n The node to be removed.
     */
    public void removeNode(Node n){
    	for (Link l : n.getLinks(true))
            this.removeLink(l);
        this.nodes.remove(n);
        n.topo=null;
        this.notifyNodeRemoved(n);
    }
    /**
     * Adds the specified link to this topology. Calling this method makes
     * sense only for wired links, since wireless links are automatically
     * managed as per the nodes' communication ranges.
     * @param l The link to be added.
     */
    public void addLink(Link l){
        this.addLink(l, false);
    }
    /**
     * Adds the specified link to this topology without notifying the listeners
     * (if silent is true). Calling this method makes sense only for wired 
     * links, since wireless links are automatically managed as per the nodes'
     * communication ranges.
     * @param l The link to be added.
     */
    public void addLink(Link l, boolean silent){
        if (l.type==Type.DIRECTED){
            arcs.add(l);
            if (arcs.contains(new Link(l.destination,l.source,Link.Type.DIRECTED))){
                Link edge=new Link(l.source,l.destination,Link.Type.UNDIRECTED,l.mode);
                edges.add(edge);
                if (!silent)
                	notifyLinkAdded(edge);
            }
        }else{
            Link arc1=new Link(l.source,l.destination,Link.Type.DIRECTED);
            Link arc2=new Link(l.destination,l.source,Link.Type.DIRECTED);
            if (!arcs.contains(arc1)){
                arcs.add(arc1);
                if (!silent)
                	notifyLinkAdded(arc1);
            }
            if (!arcs.contains(arc2)){
                arcs.add(arc2);
                if (!silent)
                	notifyLinkAdded(arc2);
            }
            edges.add(l);
        }
        if (!silent)
        	notifyLinkAdded(l);
    }
    /**
     * Removes the specified link from this topology. Calling this method makes
     * sense only for wired links, since wireless links are automatically
     * managed as per the nodes' communication ranges.
     * @param l The link to be removed.
     */
    public void removeLink(Link l){
        if (l.type==Type.DIRECTED){
            arcs.remove(l);
            Link edge=getLink(l.source, l.destination, false);
            if (edge!=null){
                edges.remove(edge);
                notifyLinkRemoved(edge);
            }
        }else{
            Link arc1=getLink(l.source, l.destination, true);
            Link arc2=getLink(l.destination, l.source, true);
            arcs.remove(arc1);
            notifyLinkRemoved(arc1);
            arcs.remove(arc2);
            notifyLinkRemoved(arc2);
            edges.remove(l);
        }
        notifyLinkRemoved(l);
    }
    /**
     * Returns a vector containing all the nodes in this topology. The returned
     * vector can be subsequently modified without effect on the topology.
     */
    public Vector<Node> getNodes(){
        return new Vector<Node>(nodes);
    }
    /** 
     * Returns a vector containing all undirected links in this topology. The 
     * returned vector can be subsequently modified without effect on the
     * topology.
     */
    public Vector<Link> getLinks(){
        return getLinks(false);
    }
    /** 
     * Returns a vector containing all links of the specified type in this
     * topology. The returned vector can be subsequently modified without
     * effect on the topology.
     * @param directed <tt>true</tt> for directed links, <tt>false</tt> for
     * undirected links.
     */
    public Vector<Link> getLinks(boolean directed){
        return (directed)?new Vector<Link>(arcs):new Vector<Link>(edges);
    }
    Vector<Link> getLinks(boolean directed, Node n, int pos){
        Vector<Link> result=new Vector<Link>();
        Vector<Link> allLinks=(directed)?arcs:edges;
        for(Link l : allLinks)
            switch(pos){
                case 0:	if(l.source==n || l.destination==n) 
                    result.add(l); break;
                case 1:	if(l.source==n)
                    result.add(l); break;
                case 2:	if(l.destination==n)
                    result.add(l); break;
            }
        return result;
    }
    /**
     * Returns the undirected link shared the specified nodes, if any.
     * @return The requested link, if such a link exists, <tt>null</tt> 
     * otherwise.
     */
    public Link getLink(Node n1, Node n2){
        return getLink(n1, n2, false);
    }
    /**
     * Returns the link of the specified type between the specified nodes, if
     * any.
     * @return The requested link, if such a link exists, <tt>null</tt> 
     * otherwise.
     */
    public Link getLink(Node from, Node to, boolean directed){
        if (directed){
            Link l=new Link(from, to,Link.Type.DIRECTED);
            return (arcs.contains(l))?arcs.elementAt(arcs.indexOf(l)):null;
        }else{
            Link l=new Link(from, to, Link.Type.UNDIRECTED);
            return (edges.contains(l))?edges.elementAt(edges.indexOf(l)):null;    		
        }
    }
    /**
     * Registers the specified topology listener to this topology. The listener
     * will be notified whenever an undirected link is added or removed.
     * @param listener The listener to add.
     */
    public void addConnectivityListener(ConnectivityListener listener){
        cxUndirectedListeners.add(listener);
    }
    /**
     * Registers the specified connectivity listener to this topology. The 
     * listener will be notified whenever a link of the specified type is 
     * added or removed.
     * @param listener The listener to register.
     * @param directed The type of links to be listened (<tt>true</tt> for 
     * directed, <tt>false</tt> for undirected).
     */
    public void addConnectivityListener(ConnectivityListener listener, boolean directed){
        if (directed)
        	cxDirectedListeners.add(listener); 
        else 
        	cxUndirectedListeners.add(listener);
    }
    /**
     * Unregisters the specified connectivity listener from the 'undirected' 
     * listeners.
     * @param listener The listener to unregister.
     */
    public void removeConnectivityListener(ConnectivityListener listener){
    	cxUndirectedListeners.remove(listener);
    }
    /**
     * Unregisters the specified connectivity listener from the listeners 
     * of the specified type.
     * @param listener The listener to unregister.
     * @param directed The type of links that this listener was listening 
     * (<tt>true</tt> for directed, <tt>false</tt> for undirected).
     */
    public void removeConnectivityListener(ConnectivityListener listener, boolean directed){
        if (directed) 
        	cxDirectedListeners.remove(listener); 
        else 
        	cxUndirectedListeners.remove(listener);
    }
    /**
     * Registers the specified movement listener to this topology. The
     * listener will be notified every time the location of a node changes. 
     * @param listener The movement listener.
     */
    public void addMovementListener(MovementListener listener){
        movementListeners.add(listener);
    }
    /**
     * Unregisters the specified movement listener for this topology.
     * @param listener The movement listener. 
     */
    public void removeMovementListener(MovementListener listener){
        movementListeners.remove(listener);
    }
    /**
     * Registers the specified topology listener to this topology. The listener
     * will be notified whenever the a node is added or removed.
     * @param listener The listener to register.
     */
    public void addTopologyListener(TopologyListener listener){
        topologyListeners.add(listener);
    }
    /**
     * Unregisters the specified topology listener.
     * @param listener The listener to unregister.
     */
    public void removeTopologyListener(TopologyListener listener){
    	topologyListeners.remove(listener);
    }
    protected void notifyLinkAdded(Link l){
    	boolean directed=(l.type==Type.DIRECTED)?true:false;
    	LinkedHashSet<ConnectivityListener> union=new LinkedHashSet<ConnectivityListener>(directed?cxDirectedListeners:cxUndirectedListeners);
    	union.addAll(directed?l.source.cxDirectedListeners:l.source.cxUndirectedListeners);
    	union.addAll(directed?l.destination.cxDirectedListeners:l.destination.cxUndirectedListeners);
    	for (ConnectivityListener cl : new Vector<ConnectivityListener>(union))
    		cl.linkAdded(l);
    }
    protected void notifyLinkRemoved(Link l){
    	boolean directed=(l.type==Type.DIRECTED)?true:false;
    	LinkedHashSet<ConnectivityListener> union=new LinkedHashSet<ConnectivityListener>(directed?cxDirectedListeners:cxUndirectedListeners);
    	union.addAll(directed?l.source.cxDirectedListeners:l.source.cxUndirectedListeners);
    	union.addAll(directed?l.destination.cxDirectedListeners:l.destination.cxUndirectedListeners);
    	for (ConnectivityListener cl : new Vector<ConnectivityListener>(union))
    		cl.linkRemoved(l);
    }
    protected void notifyNodeAdded(Node node){
        Vector<TopologyListener> listeners=new Vector<TopologyListener>(topologyListeners);
        for (TopologyListener tl : listeners)
        	tl.nodeAdded(node);
    }
    protected void notifyNodeRemoved(Node node){
        Vector<TopologyListener> listeners=new Vector<TopologyListener>(topologyListeners);
        for (TopologyListener tl : listeners)
        	tl.nodeRemoved(node);
    }
    void updateWirelessLinksFor(Node n){
        for (Node n2 : nodes)
        	if (n2!=n){
        		this.updateWirelessLink(n, n2);
        		this.updateWirelessLink(n2, n);
        	}
    }    
    void updateWirelessLink(Node n1, Node n2){
    	Link l=n1.getOutLinkTo(n2);
    	if (l==null){
    		if(n1.distance(n2)<n1.communicationRange)
    			this.addLink(new Link(n1,n2,Type.DIRECTED,Mode.WIRELESS));
    	}else
    		if (l.isWireless() && n1.distance(n2)>n1.communicationRange)
    			this.removeLink(l);
    }
    /**
     * Returns a string representation of this topology. The output of this
     * method can be subsequently used to reconstruct a topology with the 
     * <tt>fromString</tt> method. Only the nodes and wired links are exported
     * here (not the topology's properties).
     */
    public String toString(){
		StringBuffer res = new StringBuffer();
		for (Node n : nodes)
			res.append(n.toString() + " " + n.coords.toString().substring(14) + "\n");
		for (Link l : getLinks())
			if (!l.isWireless())
				res.append(l.toString()+ "\n");
		return res.toString();
	}
    /**
     * Imports nodes and wired links from the specified string representation of a 
     * topology.
     * @param s The string representation.
     */
    public void fromString(String s){
		HashMap<String,Node> nodeTable=new HashMap<String,Node>();
    	while(s.indexOf("[")>0){	
    		Node n=new Node();
    		String id=s.substring(0, s.indexOf(" "));
    		n.setProperty("id", id);
    		nodeTable.put(id, n);
    		addNode(new Double(s.substring(s.indexOf("[")+1,s.indexOf(","))),new Double(s.substring(s.indexOf(",")+2,s.indexOf("]"))),n);
    		s=s.substring(s.indexOf("\n")+1);
    	}
    	while(s.indexOf("--")>0){
    		Node n1=nodeTable.get(s.substring(0,s.indexOf(" ")));
    		Node n2=nodeTable.get(s.substring(s.indexOf(">")+2,s.indexOf("\n")));
    		Type type=(s.indexOf("<")>0 && s.indexOf("<")<s.indexOf("\n"))?Type.UNDIRECTED:Type.DIRECTED;
    		addLink(new Link(n1,n2,type,Link.Mode.WIRED));
    		s=s.substring(s.indexOf("\n")+1);
    	}
    }
}
