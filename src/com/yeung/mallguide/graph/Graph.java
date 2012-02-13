package com.yeung.mallguide.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;

public class Graph {
	//public LinkedList<GraphNode> nodes;
	public double minLat, minLon, maxLat, maxLon;
	public LinkedList<GraphEdge> edges;
	private ArrayList<GraphWay> buildings;
	private GraphNode[] nodes;
	
	public Graph(){
		//nodes = new LinkedList<GraphNode>();
		edges = new LinkedList<GraphEdge>();
		buildings = new ArrayList<GraphWay>();
	}
	
	public ArrayList<GraphWay> getBuildings(){
		return buildings;
	}
	
	public boolean loadGraphFromXML(XmlResourceParser xrp) throws XmlPullParserException, IOException{
		if(xrp == null){
			return false;
		}
		boolean ret = false;
		GraphNode tempNode = null;
		GraphWay tempWay = null;
		ArrayList<GraphNode> inputNodes = new ArrayList<GraphNode>();
		ArrayList<GraphWay> inputWays = new ArrayList<GraphWay>();
		//ArrayList<GraphWay> buildings = new ArrayList<GraphWay>();
		int eventType = xrp.next();
		boolean isOsmData = false;
		while(eventType != XmlPullParser.END_DOCUMENT){
			switch(eventType){
			case XmlPullParser.START_TAG:
				if(!isOsmData){//make sure it's in osm format, otherwise return false
					if(xrp.getName().equals("osm"))
						isOsmData = true;
					else
						return false;
				}
				else if(xrp.getName().equals("bounds")){
					int attributeCount = xrp.getAttributeCount();
					for(int i=0; i<attributeCount; ++i){
						if(xrp.getAttributeName(i).equals("minlat")){
							minLat = Double.parseDouble(xrp.getAttributeValue(i));
						}
						else if(xrp.getAttributeName(i).equals("minlon")){
							minLon = Double.parseDouble(xrp.getAttributeValue(i));
						}
						else if(xrp.getAttributeName(i).equals("maxlat")){
							maxLat = Double.parseDouble(xrp.getAttributeValue(i));
						}
						else if(xrp.getAttributeName(i).equals("maxlon")){
							maxLon = Double.parseDouble(xrp.getAttributeValue(i));
						}
					} 
				}
				else if(xrp.getName().equals("node")){
					tempNode = new GraphNode();
					if(!tempNode.parseXml(xrp)) 
						return false;
					inputNodes.add(tempNode);
				}				
				else if(xrp.getName().equals("way")){
					tempWay = new GraphWay();
					if(!tempWay.parseXml(xrp)) 
						return false;
					if(tempWay.getType() == 0)
						inputWays.add(tempWay);
					else
						buildings.add(tempWay);
				}
				break;
			case XmlPullParser.END_TAG:
				if(xrp.getName().equals("osm")){
					ret = true;
				}
				break;
			default:
				break;
			}
			eventType = xrp.next();
		}
		nodes = new GraphNode[inputNodes.size()];
		int c=0;
		for(GraphNode n : inputNodes){
			nodes[c] = n;
			++c;
		}
		sortNodesById(nodes);
		resolveRef(inputWays);
		resolveRef(buildings);
		//to be continued
		
		return ret;
	}
	
	//convert array refs to nodes
	private void resolveRef(ArrayList<GraphWay> ways){
		for(int i=0; i<ways.size(); ++i){
			ArrayList<Integer> refs = ways.get(i).getRefs();
			for(int j=0; j<refs.size(); ++j){
				GraphNode nd = getNode(refs.get(j).intValue());
				ways.get(i).addNode(nd);
			}
			refs.clear();
		}
	}
	
	//binary search a node in array "nodes"
	private GraphNode getNode(int id){
		int l, r;
		l = 0;
		r = nodes.length-1;
		while(l<=r){
			int m = (l+r)/2;
			if(nodes[m].getId()==id)
				return nodes[m];
			else if(nodes[m].getId()<id){
				l = m+1;
			}
			else
				r = m-1;
		}
		return null;
	}
	
	//insertion sort, seems that id is already sorted in osm file
	//so this one should be fast and ensure it is really sorted.
	private void sortNodesById(GraphNode[] nodes){
		for(int i=1; i<nodes.length; ++i){
			GraphNode temp = nodes[i];
			int j=i-1;
			while(temp.getId()<nodes[j].getId()&&j>0){
				nodes[j+1]=nodes[j];
				--j;
			}
			if(j!=i-1) nodes[j+1] = temp;
		}
	}
	
	//quicksort in case id is not sorted in osm
	//overload function
	private void sortNodesById(GraphNode[] nodes, int l, int r){
		if(l>=r) return;
		int i=-1;
		int temp = nodes[r].getId();
		for(int j=0; j<r; ++j){
			if(nodes[j].getId()<temp){
				++i;
				GraphNode n = nodes[j];
				nodes[j] = nodes[i];
				nodes[i] = n;				
			}
		}
		++i;
		GraphNode n = nodes[r];
		nodes[r] = nodes[i];
		nodes[i] = n;
		//till now, partition is finished
		sortNodesById(nodes, l, i-1);
		sortNodesById(nodes, i+1, r);
	}
	
	
}
