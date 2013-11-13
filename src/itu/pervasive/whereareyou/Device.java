package itu.pervasive.whereareyou;

public class Device {

	public boolean found = true;
	public double distance;
	public String s_orientation;
	/* x =  1 -> east
	 * x = -1 -> west
	 * y =  1 -> north
	 * y = -1 -> south
	 */
	public int[] orientation;
	public boolean accurate;
	public String name;
	public String mac_address;
	
	public Device(String name, String mac_address){
		this.name = name;
		this.mac_address = mac_address;
		found = false;
		distance = 0;
		s_orientation = "Not calculated";
		orientation = new int[2];
	}
	
	public void setFound(boolean found){
		this.found = found;
	}
	
	public void setDistance(double distance){
		this.distance = distance;
	}
	
	public void setSOrientation(String orientation){
		this.s_orientation = orientation;
	}
	
	public void setIOrientation(int[] orientation){
		this.orientation = orientation;
	}
	
	public void setAccuracy(boolean accurate){
		this.accurate = accurate;
	}
}
