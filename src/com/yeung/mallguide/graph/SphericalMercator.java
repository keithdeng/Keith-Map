package com.yeung.mallguide.graph;

public class SphericalMercator {
	private static final int r = 6378137;
	private static final double scale = (Math.PI*r)/180;
	
	public static double lon2x(double lon){
		return lon*scale;
	}
	
	public static double lat2y(double lat){
		return r*Math.log(Math.tan(Math.PI/4+Math.toRadians(lat)/2));
	}
	
	public static double x2lon(double x){
		return x/scale;
	}
	
	public static double y2lat(double y){
		return 2* Math.atan(Math.exp(Math.toRadians(y/r))) - Math.PI/2;
	}
}
