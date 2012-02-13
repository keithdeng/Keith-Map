package com.yeung.mallguide;

import java.util.ArrayList;
import java.util.Iterator;

import com.yeung.mallguide.graph.Graph;
import com.yeung.mallguide.graph.GraphNode;
import com.yeung.mallguide.graph.GraphWay;
import com.yeung.mallguide.graph.SphericalMercator;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public class MapView extends View{
	private boolean isLoad; //map is drawn only when it is loaded
	
	private float leftBound;
	private float rightBound;
	private float topBound;
	private float bottomBound;
	private float mapWidth; //actual size the map represents
	private float mapHeight; //actual size the map represents
	private float mapViewWidth;
	private float mapViewHeight;
	
	private Paint shapePaint;
	private Paint[] paints;
	private Paint mapPaint;
	//private Bitmap map;
	private Bitmap[] tiles;
	private int xCount, yCount;
	private ArrayList<Room>[] roomArrays;
	
	private boolean isLongPressed;
	private float zoomStartY; //zoomStartX, 
	private float relativeScale; // the scale of the map relative to zoom out
	private float refScale;
	private float scale;
	
	private Room mallFloor;
	private ArrayList<Room> rooms;
	private float ox, oy; //left-top coordinate on the view's canvas
	private int tileX, tileY;
	private GestureDetector mAction;
	
	private class Room{
		public Path path;
		public Paint paint;
		public float left, right, top, bottom;
		public Room(){
			path = new Path();
			paint = null;//paint will use customized paint according to the type
		}
		/*public boolean isOverlap(float left, float top, float right, float bottom){
			boolean x = this.right<left || this.left>right;
			boolean y = this.bottom<top || this.top>bottom;
			return !(x || y);
		}*///seems not necessary
	}
	
	public MapView(Context c){
		this(c, null, 0);
	}
	
	public MapView(Context c, AttributeSet a){
		this(c, a, 0);
	}

	public MapView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		init();
		mAction = new GestureDetector(context, new ActionListener() );		
	}
	
	private void init(){
		isLoad = false;
		rooms = new ArrayList<Room>();
		roomArrays = null;
		relativeScale = 1;
		//ox=oy=0;
		ox = -1;
		tileX = tileY = -1;

		shapePaint = new Paint();
		shapePaint.setAntiAlias(true);
		shapePaint.setColor(Color.BLACK);
		shapePaint.setStyle(Paint.Style.STROKE);
		//shapePaint.setStrokeWidth(1f);
		paints = new Paint[10];
		int c[]={Color.WHITE, Color.BLUE, Color.GRAY, Color.DKGRAY};
		for(int i=0; i<4; ++i){
			paints[i] = new Paint();
			paints[i].setAntiAlias(true);
			paints[i].setColor(c[i]);
			paints[i].setStyle(Paint.Style.FILL);
		}
		mapPaint = new Paint();
		mapPaint.setFilterBitmap(true);
		
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		//int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		
		//int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(widthSize, heightSize);
	}
	
	//get boundary from graph
	private void setBoundary(Graph g){
		leftBound = (float)SphericalMercator.lon2x(g.minLon);
		rightBound = (float)SphericalMercator.lon2x(g.maxLon);
		topBound = (float)SphericalMercator.lat2y(g.maxLat);
		bottomBound = (float)SphericalMercator.lat2y(g.minLat);
		mapWidth = rightBound - leftBound;
		mapHeight = topBound - bottomBound;
	}
	
	public void loadMap(Graph g){
		setBoundary(g);
		ArrayList<GraphWay> buildings = g.getBuildings();
		//setBoundary(buildings);
		for(int i=0; i<buildings.size(); ++i){
			GraphWay building = buildings.get(i);
			Room room = new Room();
			
			//draw path according to the nodes in the building
			ArrayList<GraphNode> nodes = building.getNodes();
			float x = (float)SphericalMercator.lon2x(nodes.get(0).getLon()) - leftBound;
			float y = topBound - (float)SphericalMercator.lat2y(nodes.get(0).getLat());
			room.left = room.right = x;
			room.top = room.bottom = y;
			room.path.moveTo(x, y);
			for(int j=1; j<nodes.size(); ++j){
				x = (float)SphericalMercator.lon2x(nodes.get(j).getLon()) - leftBound;
				y = topBound - (float)SphericalMercator.lat2y(nodes.get(j).getLat());
				room.path.lineTo(x, y);
				if(room.left>x){
					room.left = x;
				}
				else if(room.right<x){
					room.right = x;
				}
				if(room.top>y){
					room.top = y;
				}
				else if(room.bottom<y){
					room.bottom = y;
				}
			}
			room.paint = paints[building.getType()-1];
			if(building.getType()==1){
				mallFloor = room;
			}
			else
				rooms.add(room);
		}
		isLoad = true;
	}
	
	//link rooms to tiles of View size 
	private void linkRoomToTiles(){
		scale = getWidth()/mapWidth;
		float tmp = getHeight()/mapHeight;
		if(scale>tmp)
			scale = tmp;
		scale *= relativeScale;
		mapViewWidth = scale*mapWidth;
		mapViewHeight = scale*mapHeight;
		xCount = (int)Math.ceil(scale*mapWidth/getWidth());
		yCount = (int)Math.ceil(scale*mapHeight/getHeight());
		int total = xCount * yCount;
		
		//if relink is performed, recycle all the current bitmaps
		if(tiles!=null){
			for(int i=0; i<tiles.length; ++i){
				if(tiles[i]!=null) 
					tiles[i].recycle();
			}
		}
		tiles = new Bitmap[total];
		//clear all arrays
		if(roomArrays!=null){
			for(int i=0; i<roomArrays.length; ++i)
				roomArrays[i].clear();
		}
		roomArrays = new ArrayList[total];
		for(int i=0; i<total; ++i){
			roomArrays[i] = new ArrayList<Room>();
			tiles[i] = null;
		}
		Iterator<Room> it = rooms.iterator();
		while(it.hasNext()){
			Room r = it.next();
			int xStart = (int)(scale*r.left/getWidth());
			int xEnd = (int)Math.ceil(scale*r.right/getWidth());
			int yStart = (int)(scale*r.top/getHeight());
			int yEnd = (int)Math.ceil(scale*r.bottom/getHeight());
			for(int x=xStart; x<xEnd; ++x){
				for(int y=yStart; y<yEnd; ++y){
					roomArrays[x*yCount+y].add(r);			
				}
			}
		}
	}
	
	
	private void drawTiles(){
		if(ox<0){
			ox = mapViewWidth/2-getWidth()/2;
			oy = mapViewHeight/2-getHeight()/2;
			if(ox<0) ox = 0;
			if(oy<0) oy = 0;
		}
		int x=(int)(ox/getWidth());
		int y=(int)(oy/getHeight());
		//Log.i("drawTile", "ox:"+ox+", oy:"+oy);
		//Log.i("drawTile", "x:"+x+", y:"+y);
		//Log.i("drawTile", "xcount:"+xCount+", ycount:"+yCount);
		//if(x==xCount-1)	--x;
		//if(y==yCount-1)	--y;
		//Log.i("drawTile(after)", "x:"+x+", y:"+y);
		if(tileX>=0){
			//delete possible previous drawn tiles
			if(tileX<x || tileX-x>1){
				int c = tileX*yCount+tileY;
				for(int i=c; i<c+2 && i<xCount; ++i){
					if(tiles[i]!=null){
						tiles[i].recycle();
						tiles[i] = null;
					}
				}
			}
			if(x<tileX || x-tileX>1){
				int c = (1+tileX)*yCount+tileY;
				for(int i=c; i<c+2 && i<xCount; ++i){
					if(tiles[i]!=null){
						tiles[i].recycle();
						tiles[i] = null;
					}
				}
			}
			if(tileY<y || tileY-y>1){
				int c = tileX*yCount+tileY;
				for(int i=c; i<=c+yCount; i+=yCount){
					if(tiles[i]!=null){
						tiles[i].recycle();
						tiles[i] = null;
					}
				}
			}
			if(y<tileY || y-tileY>1){
				int c = tileX*yCount+tileY+1;
				for(int i=c; i<=c+yCount; i+=yCount){
					if(tiles[i]!=null){
						tiles[i].recycle();
						tiles[i] = null;
					}
				}
			}			
		}
		tileX = x;
		tileY = y;
		
		//draw necessary tiles
		for(int i=x; i<x+2 && i<xCount; ++i){
			for(int j=y; j<y+2 && j<yCount; ++j){
				int c = i*yCount+j;
				
				if(tiles[c] == null){
					//draw tile
					tiles[c] = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
					Canvas canvas = new Canvas(tiles[c]);
					canvas.drawColor(0x0);
					canvas.translate(-i*getWidth(), -j*getHeight());
					canvas.scale(scale, scale);
					canvas.drawPath(mallFloor.path,mallFloor.paint);
					canvas.drawPath(mallFloor.path, shapePaint);
					Iterator<Room> it = roomArrays[c].iterator();
					while(it.hasNext()){
						Room r = it.next();
						canvas.drawPath(r.path, r.paint);
						canvas.drawPath(r.path, shapePaint);
					}
				}
			}
		}
		invalidate();		
	}
	

	//make sure moves in the boundary
	private void validateXY(float dx, float dy){
		float oldX = ox;
		float oldY = oy;
		ox+=dx;
		oy+=dy;
		float tmp = mapViewWidth-getWidth(); 
		if(ox > tmp)
			ox = tmp;			
		tmp = mapViewHeight-getHeight();
		if(oy > tmp)
			oy = tmp;
		if(ox<0)
			ox = 0;
		if(oy<0)
			oy = 0;
		//Log.i("move", "ox: "+ox+", "+ox/getWidth());
		//Log.i("move", "oy: "+oy+", "+oy/getHeight());
		if(oldX != ox || oldY != oy);
			drawTiles();		
	}
	
	private void zoom(float y){
		float dy = y-zoomStartY;
		Log.i("zoom", ""+dy);
		float oldScale = relativeScale;
		
		//zoom based on refScale
		relativeScale = refScale*(float)Math.pow(2, -dy/100);
		if(relativeScale<1)
			relativeScale = 1;
		if(relativeScale>8)
			relativeScale = 8;
		
		//adjust ox, oy such that the screen center is fixed
		ox += getWidth()/2;
		oy += getHeight()/2;
		ox *=relativeScale/oldScale;
		oy *=relativeScale/oldScale;
		ox -= getWidth()/2;
		oy -= getHeight()/2;
		
		tileX = -1;//this is to avoid deletion process in drawTiles()
		//redraw the tiles
		linkRoomToTiles();
		drawTiles();
	}
	
	public void save(Editor e){		
		e.putFloat("ox", ox);
		e.putFloat("oy", oy);
		e.commit();
	}
	public void resume(SharedPreferences s){
		ox = s.getFloat("ox", 0);
		oy = s.getFloat("oy", 0);
		Log.i("resume", "ox:"+ox+", oy:"+oy);
	}
	
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh){
		//drawMap();
		linkRoomToTiles();
		drawTiles();
	}
	
	@Override
	protected void onDraw(Canvas canvas){
		if(isLoad){
			
			/*Rect src = new Rect((int)(-ox/relativeScale),
					(int)(-oy/relativeScale),
					(int)((-ox+getWidth())/relativeScale),
					(int)((-oy+getHeight())/relativeScale) );
			Rect dst = new Rect(0,0,getWidth(), getHeight());
			canvas.drawBitmap(map, src, dst, mapPaint);*/
			drawView(canvas);
		}
	}
	
	private void drawView(Canvas canvas){
		canvas.drawColor(Color.LTGRAY);
		for(int x=tileX; x<tileX+2 && x<xCount; ++x){
			for(int y=tileY; y<tileY+2 && y<yCount; ++y){
				canvas.save();
				canvas.translate(x*getWidth()-ox, //getWidth()-(x*getWidth()-ox), 
						y*getHeight()-oy);
				canvas.drawBitmap(tiles[x*yCount+y], 0,0, null);
				canvas.restore();
			}
		}
		
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if(mAction.onTouchEvent(ev)){
			return true;
		}
		else{
			if(ev.getAction()==MotionEvent.ACTION_MOVE && isLongPressed){
				Log.i("special move", "oy:"+zoomStartY+", y:"+ev.getY());
				zoom(ev.getY());
				return true;
			}
			if(ev.getAction()==MotionEvent.ACTION_UP){
				isLongPressed = false;
				return true;
			}
			return false;
		}
	}
	
	
	private class ActionListener implements OnGestureListener{
		
		@Override
		public boolean onDown(MotionEvent arg0) {
			// TODO Auto-generated method stub
			isLongPressed = false;
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub
			//zoomStartX = e.getX();
			isLongPressed = true;
			zoomStartY = e.getY();
			refScale = relativeScale;
			Log.i("longpress", ""+zoomStartY);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float dx, float dy) {
			// TODO Auto-generated method stub
			//ox += dx;
			//oy += dy;
			Log.i("scroll", "yes");
			validateXY(dx, dy);
			//invalidate();
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
	
	/*private void drawTiles(){
	float scale = getWidth()/mapWidth;
	float tmp = getHeight()/mapHeight;
	if(scale>tmp)
		scale = tmp;
	scale *= relativeScale;
	mapViewWidth = scale*mapWidth;
	mapViewHeight = scale*mapHeight;
	xCount = (int)Math.ceil(scale*mapWidth/getWidth());
	yCount = (int)Math.ceil(scale*mapHeight/getHeight());
	tiles = new Bitmap[xCount*yCount];
	int c = 0;
	for(int i=0; i<xCount; ++i){
		for(int j=0; j<yCount; ++j){
			tiles[c] = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(tiles[c]);
			canvas.drawColor(Color.WHITE);
			canvas.translate(-i*getWidth(), -j*getHeight());
			canvas.scale((float)scale, (float)scale);
			for(int k=0; k<rooms.size(); ++k){
				canvas.drawPath(rooms.get(k).path, rooms.get(k).paint);
				canvas.drawPath(rooms.get(k).path, shapePaint);
			}
			++c;
		}
	}
	invalidate();		
}*/
	
	//draw
	/*private void drawMap(){
		if(map != null){
			map.recycle();
		}
		float scale = getWidth()/mapWidth;
		float tmp = getHeight()/mapHeight;
		if(scale>tmp)
			scale = tmp;
		map = Bitmap.createBitmap((int)(mapWidth*scale), (int)(mapHeight*scale), Bitmap.Config.ARGB_8888);
		//scale *= relativeScale;
		shapePaint.setStrokeWidth((float)(2f/scale));
		
		//map = Bitmap.createBitmap((int)(mapWidth*scale), (int)(mapHeight*scale), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(map);
		canvas.drawColor(Color.WHITE);
		canvas.scale((float)scale, (float)scale);
		for(int i=0; i<rooms.size(); ++i){
			canvas.drawPath(rooms.get(i).path, rooms.get(i).paint);
			canvas.drawPath(rooms.get(i).path, shapePaint);
		}
		//translate.postTranslate(-getWidth()/2, -getHeight()/2);
		invalidate();
	}*/	
}
