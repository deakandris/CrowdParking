package hu.bme.tmit.deakandras.crowdparking.data;

import java.util.Date;


/** A parking history of the road segment.
 * @author András Deák
 *
 */
public class ParkingSpotHistory {
	private int freed;
	private int occupied;
	private Date startTime;
	private Date endTime;
	
	/**
	 * @param freed The number of parking spots freed in the given time interval.
	 * @param occupied The number of parking spots being occupied in the given time interval.
	 * @param startTime The start time of the time interval.
	 * @param endTime The end time of the time interval.
	 */
	public ParkingSpotHistory(int freed, int occupied, Date startTime, Date endTime){
		this.freed = freed;
		this.occupied = occupied;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	/**
	 * @return The number of parking spots freed in the given time interval.
	 */
	public int getFreed() {
		return freed;
	}

	/**
	 * @return The number of parking spots being occupied in the given time interval.
	 */
	public int getOccupied() {
		return occupied;
	}

	/**
	 * @return The start time of the time interval.
	 */
	public Date getStartTime() {
		return startTime;
	}

	/**
	 * @return The end time of the time interval.
	 */
	public Date getEndTime() {
		return endTime;
	}
	
}
