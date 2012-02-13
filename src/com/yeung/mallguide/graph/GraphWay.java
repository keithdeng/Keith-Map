package com.yeung.mallguide.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.R.integer;
import android.content.res.XmlResourceParser;

//add variable type, may need to add functions to access it

public class GraphWay {
	// all nodes on this path ( ref0 -> ref1 -> ref2  -> ...)
	private ArrayList<GraphNode> nodes;
	private ArrayList<Integer> refs;
	private int id;
	private short wheelchair;
	// 0: way, 1: a building, 2-n: different type of room
	private short type;

	// >0 := number correct steps given
	//  0 := no steps
	// -1 := undefined number of steps
	// -2 := elevator
	private int numSteps = 0;	
	
	// Float.MAX_VALUE == undefined!
	private float level;
	private boolean isIndoor;
	
	
	/**
	 * Constructor to create an empty way.
	 */
	public GraphWay() {
		this.nodes = new ArrayList<GraphNode>();
		refs = new ArrayList<Integer>();
		this.id = 0;
		this.wheelchair = 1;
		this.level = Float.MAX_VALUE;
		this.type = 0;
	}
	
	/**
	 * Constructor to create a coordinate with given parameters.
	 * 
	 * @param refs  a LinkedList of Integers, references to GraphNodes
	 * @param id the id of this way
	 * @param wheelchair the value concerning the wheelchair attribute
	 * @param level the level of this way
	 */
	public GraphWay(ArrayList<Integer> refs, int id, short wheelchair, float level) {
		this.refs = refs;
		this.id = id;
		this.wheelchair = wheelchair;
		this.level = level;
	}
	
	public boolean parseXml(XmlResourceParser xrp) throws XmlPullParserException, IOException{
		int attributeCount = xrp.getAttributeCount();
		for(int i=0; i<attributeCount; ++i){
			if(xrp.getAttributeName(i).equals("id")){
				id = xrp.getAttributeIntValue(i,0);
			}
		}
		int eventType = xrp.next();
		while(eventType != XmlPullParser.END_DOCUMENT){
			attributeCount = xrp.getAttributeCount();
			switch(eventType){
			case XmlPullParser.START_TAG:
				if(xrp.getName().equals("tag")){					
					for(int i = 0; i < attributeCount; i++){
						if(xrp.getAttributeName(i).equals("k")
								&& xrp.getAttributeValue(i).equals("wheelchair")){		// way.tag.wheelchair
							String v = xrp.getAttributeValue(i + 1);
							wheelchair = (short) (v.equals("yes") ? 1 : v.equals("limited")?0: -1);
						} else if(xrp.getAttributeName(i).equals("k")
								&& xrp.getAttributeValue(i).equals("step_count")){		// way.tag.step_count
							numSteps = xrp.getAttributeIntValue(i + 1, Integer.MAX_VALUE);
						} else if(xrp.getAttributeName(i).equals("k")
								&& xrp.getAttributeValue(i).equals("level")){			// way.tag.level
							String v = xrp.getAttributeValue(i + 1);
							level = Float.parseFloat(v);
						} else if(xrp.getAttributeName(i).equals("k")
								&& xrp.getAttributeValue(i).equals("indoor")){			// way.tag.indoor
							String v = xrp.getAttributeValue(i + 1);
							isIndoor = v.equals("yes");
						}else if(xrp.getAttributeName(i).equals("k")
								&& xrp.getAttributeValue(i).equals("highway")){			// way.tag.highway
							String v = xrp.getAttributeValue(i + 1);
							if(v.equals("steps")){
								wheelchair = -1;
								if(numSteps == 0){ 	// no steps configured before
									numSteps = -1;	 	// so set to undefined (but present),
																// otherwise might be set later
								}
							}
							if(v.equals("elevator")){
								wheelchair = 1;
								numSteps = -2;
							}
						}
						else if(xrp.getAttributeName(i).equals("k")
								&& xrp.getAttributeValue(i).equals("type")){//building is represented by way
							type = (short)xrp.getAttributeIntValue(i+1, 0);							
						}						
					}
				}
				else if(xrp.getName().equals("nd")){
					for(int i=0; i<attributeCount; ++i){
						if(xrp.getAttributeName(i).equals("ref")){
							int x = xrp.getAttributeIntValue(i, 0);
							refs.add(new Integer(x));				
						}
					}	
				}
				break;
			case XmlPullParser.END_TAG:
				if(xrp.getName().equals("way")){
					return true;
				}
				break;
			}
			eventType = xrp.next();
		}
		return false;
	}
	
	public ArrayList<GraphNode> getNodes() {
		return nodes;
	}
	
	public ArrayList<Integer> getRefs(){
		return refs;
	}
	
	public int getId() {
		return id;
	}
	
	public short getWheelchair() {
		return wheelchair;
	}
	
	public int getSteps(){
		return numSteps;
	}
	
	public float getLevel(){
		return level;
	}
	
	public short getType(){
		return type;
	}
	
	public boolean isIndoor(){
		return isIndoor;
	}
	
	/*public void setRefs(ArrayList<GraphNode> refs) {
		this.refs = refs;
	}*/
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setWheelchair(short wheelchair) {
		this.wheelchair = wheelchair;
	}
	
	public void setSteps(int numSteps){
		this.numSteps = numSteps;
	}
	
	public void setLevel(float level){
		this.level = level;
	}
	
	public void setIndoor(boolean isIndoor){
		this.isIndoor = isIndoor;
	}
	
	public void addNode(GraphNode n){
		nodes.add(n);
	}
	
	public String toString(){
		String ret = "\nWay(" + this.id +"): ";
		ret += this.wheelchair >= 0 ? "(wheelchair)" : "(non-wheelchair)";
		ret += "\nRefs:";
		for(GraphNode ref: nodes){
			ret += "\n    " + ref.getId();
		}
		return ret;
	}
}