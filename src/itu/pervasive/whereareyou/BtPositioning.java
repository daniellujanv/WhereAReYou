package itu.pervasive.whereareyou;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.util.Log;

public class BtPositioning extends Activity{

	public List<Double> readings; //raw readings
	public List<Double> meanReadings; // readings after mean filter
	public List<Double> maReadings; // moving average 
	private double[] weights = new double[]{1 ,3 ,5 ,7 ,9 ,7 ,5 ,3 ,1};
	public List<Double> distances; //distances for mean readings
	public List<Double> avg_distances;
	public double avg_distance = 0;
	public String calculated_orientation = "Not Calculated";
	public int orientation[]; //for displaying with ARActivity x = east|west y = north|south
	public boolean accurate = true;

	public BtPositioning(){
		readings = new ArrayList<Double>();
		meanReadings = new ArrayList<Double>();
		distances = new ArrayList<Double>();
		avg_distances = new ArrayList<Double>();
		orientation = new int[]{0,0};
		

		orientation[0] = 0;
		orientation[1] = -1;
		avg_distance = 10.0;
	}

	public double getDistance(double rssi_reading){
		double dBmi = rssi_reading;
		double dBm0 = 40;
		double distance;
		double d0 = 1;
		double n = 2.5;
		double A = 10,N; //variables of formula to solve for X in LOG enpressions LogA(distance)=N
		/**
		 *	DBm(i) = DBm(0) + 10n*Log10(di/d0)
		 *
		 *     dBmi - dBm0
		 *   [ ___________]= Log10(distance/d0)
		 *        10*n
		 * 
		 *   N = LogA(distance/d0)
		 *       
		 *    		dBmi - dBm0
		 *   N =  [ ___________] , A = 10 , distance = d, d0 = initial distance
		 *        		10*n
		 * 
		 *  (A^N)*d0 = d
		 */
		if(Math.abs(dBmi) <= 77){ // 0-3 mts
			n = 2.0;
			d0 = 1;
			dBm0 = 55;
			//			Log.w("n","< 77");
		}else 
			/*	
			if(Math.abs(dBmi) < 74){ //4 - 10 mts
			n = 3.0;
			d0 = 4;
			dBm0 = -65;
			Log.w("n","< 74");
		}else 
			 */
		{
			n = 4.0;
			d0 = 13;
			dBm0 = 76;
			//			Log.w("n","> 77");
		}

		N = (Math.abs(dBmi) - dBm0);
		N = N / (10*n);
		distance = Math.pow(A, N)*d0;
		return distance;
	}

	public double addReading(double reading){
		readings.add(reading);
		return getDistance(reading); 
	}

	public void calculateDistances(){
		meanFilter();//first the mean filter is applied and the distances are calculated over that
		meanFilter();//double filter
		double avg_6 = 0.0;
		for(int i=0; i< meanReadings.size();i++){
			distances.add(getDistance(meanReadings.get(i)));
			if(i%6 == 0 && i > 0){
				avg_distances.add(avg_6/6);
				avg_6 = 0;
			}else{
				avg_6 += distances.get(i);
			}
		}
	}

	public void calculateOrientation(){
		/*
		 * readings always start heading north
		 * 				0
		 * 				N
		 * 		7		.		1
		 * 			NW	.	NE
		 * 				.
		 *	6	W	.	.	.	E 2
		 * 				.
		 * 			SW	.	SE
		 * 		5		.		3
		 * 				S
		 * 				4
		 * Readings:
		 * 00 - 05 = N
		 * 06 - 11 = NE
		 * 12 - 17 = E
		 * 18 - 23 = SE
		 * 24 - 29 = S
		 * 30 - 35 = SW
		 * 36 - 41 = W
		 * 42 - 47 = NW
		 *
		 * If the highest reading points north the lowest must point south
		 * 
		 */
		String[] orientations = new String[]{"North","North-East","East",
				"South-East","South","South-West","West","North-West"};
		//		String h_orientation;//string for heighest orientation
		//		String l_orientation; //string for lowest orientation
		int h_reading = 0; //index for heighest reading
		int l_reading = 0; // index for lowest reading
		accurate = true;
		for(int i=0;i < distances.size() ; i++){
			for(int j = i+1; j < distances.size()-1; j++){
				if(distances.get(h_reading) < distances.get(j) ){
					h_reading = j;
				}
				if(distances.get(l_reading) > distances.get(j)){
					l_reading = j;
				}
			}
		}
		int index_h = (int) Math.floor(h_reading/6);
		int index_l = (int) Math.floor(l_reading/6);
		calculated_orientation = orientations[index_l];
		//calculate index for orientation if index_l = 4 (south) 
		//we need to know if index_h points to North
		if( add(index_h, 4, orientations.length) == index_l || add(index_h, 3, orientations.length) == index_l || add(index_h, 5, orientations.length) == index_l){
			accurate = true;
		}else{
			accurate = false;
		}
		
		//calculate average distance with the 4 lowest
		double average = 0.0;
		average += distances.get(index_l); //length 60???????
		average += distances.get(add(index_l,-1, distances.size()));
		average += distances.get(add(index_l,+1, distances.size()));
		average += distances.get(add(index_l,+2, distances.size()));
		average = average/4;
		avg_distance = average;

		/* x =  1 -> east
		 * x = -1 -> west
		 * y =  1 -> north
		 * y = -1 -> south
		 * 0 <= index_l <= 7
		 */
		switch(index_l){
		case 0:
			orientation[0] = 0;
			orientation[1] = 1;
			;
			break;
		case 1:
			orientation[0] = 1;
			orientation[1] = 1;
			;
			break;
		case 2: 
			orientation[0] = 1;
			orientation[1] = 0;
			;
			break;
		case 3: 
			orientation[0] = 1;
			orientation[1] = -1;
			;
			break;
		case 4: 
			orientation[0] = 0;
			orientation[1] = -1;
			;
			break;
		case 5:
			orientation[0] = -1;
			orientation[1] = -1;
			;
			break;
		case 6: 
			orientation[0] = -1;
			orientation[1] = 0;
			;
			break;
		case 7: 
			orientation[0] = -1;
			orientation[1] = 1;
			;
			break;
		default: 
			orientation[0] = 0;
			orientation[1] = 0;
			;
			break;
		}
	}

	private int add(int index, int toAdd, int listSize){
		if(index + toAdd > listSize){
			return (index + toAdd - 1 - listSize);
		}else if(index + toAdd < 0){
			return (toAdd + index + listSize);
		}else{
			return index + toAdd;
		}
	}

	public int[] getOrientation(){
		return orientation;
	}

	public String getSOrientation(){
		return this.calculated_orientation;
	}
	
	public boolean getAccuracy(){
		return accurate;
	}
	
	private void meanFilter(){
		//how many digits around the one being processed are taken into account
		double[] digitsAround = new double[weights.length];
		//digits to "include" if processing the initial/final part of list
		int offset = (weights.length / 3);
		int denominator = 0;
		for(double r : weights){
			denominator += r;
		}
		meanReadings.clear();
		meanReadings = new ArrayList<Double>();
		for(int i = 0; i < readings.size(); i++){
			for(int j = 0; j <  weights.length; j++){
				if((i+j) - offset < 0 || (i+j) - offset >= readings.size()){
					digitsAround[j] = (double)readings.get(i); // adding numbers around the list to complete mean filter
				}
				else{
					digitsAround[j] = (double)readings.get((i+j)-offset);
				}
			}
			float temporary = 0;
			//multiply by weights
			for(int g = 0; g < digitsAround.length; g++){
				temporary += digitsAround[g] * weights[g];
			}
			//get smoothed number
			meanReadings.add((double)(temporary / denominator));
		}
		readings.clear();
		readings = new ArrayList<Double>(meanReadings);
	}

	public String getCalculatedDistances(){
		String response = "****\n AVG Distances \n";
		for(int i = 0; i < avg_distances.size(); i++){
			response += i+ " - "+ String.format("%.3f",avg_distances.get(i)) + " \n ";
		}
		response += "Average " + String.format("%.3f", avg_distance) + "\n Orientation - \n"+ calculated_orientation;
		Log.w("ALL", response);
		return response;
	}

	public double getCalculatedDistance(){
		return avg_distance;
	}

	public void printList(List<Double> list, String it){
		String toPrint = it;
		for(int i=0;i<list.size();i++){
			toPrint += ","+list.get(i).toString();
		}
		toPrint+="\n";
		Log.w("mean",toPrint);
	}
}
