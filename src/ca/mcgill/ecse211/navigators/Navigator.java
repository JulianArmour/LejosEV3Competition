package ca.mcgill.ecse211.navigators;

import ca.mcgill.ecse211.Main;
import ca.mcgill.ecse211.localizers.Localization;
import ca.mcgill.ecse211.odometer.*;
import ca.mcgill.ecse211.strategies.Beeper;
import ca.mcgill.ecse211.strategies.CanSearch;
import lejos.hardware.Sound;

/**
 * Provides the methods for navigation tasks
 * 
 * @author Cedric
 * @since 5th of March 2019
 * @version 1
 */
public class Navigator {
    // constants

    // dependencies
    private MovementController move;
    private Odometer           odo;
    private Localization       localizer;
    // fields
    private float            tileSize;
    private int                TLLX, TLLY, TURX, TURY, STZLLX, STZLLY, STZURX, STZURY, ILLX, ILLY, IURX, IURY;
    private int                dumpingSpotX, dumpingSpotY;
    private int                bridgeTileLength;
    private int                SC;
    private double             lightSensorToWheelbase;
    private int[]              searchZoneLL;
    private int[]              searchZoneUR;

    /**
     * @param move
     * @param odo
     * @param localizer
     * @param tunnelLL
     * @param TUR
     * @param STZLL
     * @param STZUR
     * @param SC
     * @param ILL
     * @param IUR
     * @param tileSize
     */
    public Navigator(
            MovementController move, Odometer odo, Localization localizer, int[] tunnelLL, int TUR[], int STZLL[],
            int STZUR[], int SC, int ILL[], int IUR[], int[] searchZoneLL, int[] searchZoneUR, float tileSize
    ) {
        this.move = move;
        this.odo = odo;
        this.TLLX = tunnelLL[0]; // Set tunnel coordinates
        this.TLLY = tunnelLL[1];
        this.TURX = TUR[0];
        this.TURY = TUR[1];
        this.STZLLX = STZLL[0]; // Set starting zone coordinates
        this.STZLLY = STZLL[1];
        this.STZURX = STZUR[0];
        this.STZURY = STZUR[1];
        this.ILLX = ILL[0]; // Set search zone coordinates
        this.ILLY = ILL[1];
        this.IURX = IUR[0];
        this.IURY = IUR[1];
        // set search zone coordinates
        this.searchZoneLL = searchZoneLL;
        this.searchZoneUR = searchZoneUR;
        this.localizer = localizer;
        this.SC = SC;
        this.lightSensorToWheelbase = Main.LT_SENSOR_TO_WHEELBASE;
        // Calculate bridge length from coordinates
        this.bridgeTileLength = (Math.abs(TLLX - TURX) > Math.abs(TLLY - TURY)) ? Math.abs(TLLX - TURX)
                : Math.abs(TLLY - TURY);
        this.tileSize = tileSize;

    }

    /**
     * Causes the robot to travel to the upper right corner of the search zone. This
     * method is meant to be called from within the search zone.
     * 
     * @author Julian Armour
     * @since March 16, 2019
     */
    public void travelToSearchZoneUR() {
        // travel to the first scan zone, which is outside the searchzone. This is a
        // safe movement since there shouldn't be any cans in the way
        float[] safePoint = CanSearch.getScanningPoints().get(0);
        move.travelTo(safePoint[0], safePoint[1], false);
        // face north and correct odometer
        move.turnTo(0);
        localizer.quickLocalization();
        // now move to the searchzone upper right's y-coordinate + half a tile
        move.travelTo(odo.getXYT()[0], (searchZoneUR[1]+0.5)*tileSize, false);
        // now move to the searchzone upper right's x-coordinate + half a tile
        // should we move right?
        if (odo.getXYT()[0] < searchZoneUR[0]) {
            // face right
            move.turnTo(90);
            localizer.quickLocalization();
            move.travelTo((searchZoneUR[0]+0.5)*tileSize, (searchZoneUR[1]+0.5)*tileSize, false);
        }
        // finally move to exact upper right
        move.travelTo(searchZoneUR[0]*tileSize, searchZoneUR[1]*tileSize, false);
        move.turnTo(move.roundAngle());
        localizer.completeQuickLocalization();
    }

    /**
     * Causes the robot to travel to the lower left corner of the search zone. This
     * method should be called after the robot crosses the tunnel and localizes.
     * 
     * @author Julian Armour
     * @since March 16, 2019
     */
    public void travelToSearchZoneLL() {
        double[] curPos = odo.getXYT();
        // travel to half a tile under searchZoneLL's y-coordinate
        move.turnTo(move.calculateAngle(curPos[0], curPos[1], curPos[0], (searchZoneLL[1]) * tileSize));
        localizer.quickLocalization();
        move.travelTo(curPos[0], (searchZoneLL[1]) * tileSize, false);
        System.out.println("ODO:\t"+"X:"+odo.getXYT()[0]/tileSize+" Y:"+odo.getXYT()[1]/tileSize);
        localizer.quickLocalization();
        System.out.println("ODO:\t"+"X:"+odo.getXYT()[0]/tileSize+" Y:"+odo.getXYT()[1]/tileSize);
        move.travelTo(curPos[0], (searchZoneLL[1] - 0.5) * tileSize, false);
        // at this point the robot is half a tile bellow the searchZoneLL's y-coordinate
        // now travel to searchZoneLL's x-coordinate
        curPos = odo.getXYT();
        // move left or right?
        if (curPos[0] < searchZoneLL[0] * tileSize) {
            // face right
            move.turnTo(90);
        } else {
            // face left
            move.turnTo(270);
        }
        // fast localization to straighten
        localizer.quickLocalization();
        // now go
        move.travelTo(searchZoneLL[0] * tileSize, (searchZoneLL[1] - 0.5) * tileSize, false);
        // now to move up half a tile in the y-direction
        
        move.travelTo(searchZoneLL[0] * tileSize, searchZoneLL[1] * tileSize, false);
        // at this point the robot should be at searchzone's lower left. Localize to
        // make sure
        localizer.completeQuickLocalization();
    }
	
    /**
     * Travels the robot to the first scanning point
     * 
     * @author Julian Armour
     * @since March 18, 2019
     */
    public void travelToSafeZone() {
        move.travelTo(CanSearch.getScanningPoints().get(0)[0], CanSearch.getScanningPoints().get(0)[1], false);
    }
    
	/**
	 * Travel to the tunnel from either the starting point or any point on the island
	 * @param direction Boolean, if true, robot is going to the tunnel from the starting zone, if false the robot is going to the tunnel from the search zone
	 */
	public void travelToTunnel(boolean direction) {

		boolean OP1 = true;
		int turnToTunnel = 0;
		double tunnelTilePosYOP2 = 0, tunnelTilePosXOP2 = 0, tunnelTilePosXOP1 = 0, tunnelTilePosYOP1 = 0;
		if(direction) { //If robot is going to tunnel in starting zone, use these parameters
			switch(SC) {
			case 0:
				if(TURX > STZURX) {
					OP1 = true;
					turnToTunnel = 90;
					tunnelTilePosXOP1 = TLLX-1;
					tunnelTilePosYOP1 = TLLY + 0.5;
				} else {
					OP1 = false;
					tunnelTilePosYOP2 = TLLY - 1;
					tunnelTilePosXOP2 = TURX - 0.5;
					turnToTunnel = 0;
				}
				break;
			case 1:
				if(TLLX < STZLLX) {
					OP1 = true;
					tunnelTilePosXOP1 = TURX + 1;
					tunnelTilePosYOP1 = TURY - 0.5;
					turnToTunnel = 270;
				} else { 
					OP1 = false;
					tunnelTilePosXOP2 = TURX - 0.5;
					tunnelTilePosYOP2 = TLLY - 1;
					turnToTunnel = 0;
				}
				break;
			case 2:
				if(TLLX < STZLLX) {
					OP1 = true;
					tunnelTilePosXOP1 = TURX + 1;
					tunnelTilePosYOP1 = TURY - 0.5;
					turnToTunnel = 270;
				} else {
					OP1 = false;
					tunnelTilePosYOP2 = TURY + 1;
					tunnelTilePosXOP2 = TURX - 0.5;
					turnToTunnel = 180;
				}
				break;
			case 3:
				if(TURX > STZURX) {
					OP1 = true;
					turnToTunnel = 90;
					tunnelTilePosXOP1 = TLLX-1;
					tunnelTilePosYOP1 = TLLY + 0.5;
				} else {
					OP1 = false;
					tunnelTilePosYOP2 = TURY + 1;
					tunnelTilePosXOP2 = TURX - 0.5;
					turnToTunnel = 180;
				}
				break;
			default:
				break;
			}
		} else { //If robot is going to tunnel from search zone, use these parameters
			switch(SC) {
			case 0:
				if(TURX > STZURX) {
					OP1 = true;
					turnToTunnel = 270;
					tunnelTilePosXOP1 = TURX + 1;
					tunnelTilePosYOP1 = TURY - 0.5;
				} else {
					OP1 = false;
					tunnelTilePosYOP2 = TURY - 0.5;
					tunnelTilePosXOP2 = TURX + 1;
					turnToTunnel = 180;
				}
				break;
			case 1:
				if(TLLX < STZLLX) {
					OP1 = true;
					tunnelTilePosXOP1 = TLLX - 1;
					tunnelTilePosYOP1 = TURY + 0.5;
					turnToTunnel = 90;
				} else { 
					OP1 = false;
					tunnelTilePosXOP2 = TURX - 0.5;
					tunnelTilePosYOP2 = TURY + 1;
					turnToTunnel = 180;
				}
				break;
			case 2:
				if(TLLX < STZLLX) {
					OP1 = true;
					tunnelTilePosXOP1 = TLLX - 1;
					tunnelTilePosYOP1 = TLLY + 0.5;
					turnToTunnel = 90;
				} else {
					OP1 = false;
					tunnelTilePosYOP2 = TLLY - 1;
					tunnelTilePosXOP2 = TLLX + 0.5;
					turnToTunnel = 0;
				}
				break;
			case 3:
				if(TURX > STZURX) {
					OP1 = true;
					turnToTunnel = 270;
					tunnelTilePosXOP1 = TURX + 1;
					tunnelTilePosYOP1 = TURY - 0.5;
				} else {
					OP1 = false;
					tunnelTilePosYOP2 = TLLY - 1;
					tunnelTilePosXOP2 = TLLX + 0.5;
					turnToTunnel = 0;
				}
				break;
			default:
				break;
			}
		}
		if(OP1) { //Path 1 to tunnel depending on tunnel position: if the tunnel is on the east or west side of the starting zone
		    //Move to the x position on the grid line before the tunnel
			move.travelTo(tunnelTilePosXOP1*tileSize, odo.getXYT()[1], false); 
		    localizer.quickLocalization();
			move.driveDistance(-lightSensorToWheelbase);
			//turn to to the y position between gridlines before the tunnel
			move.turnTo(move.calculateAngle(odo.getXYT()[0], odo.getXYT()[1], odo.getXYT()[0], tunnelTilePosYOP1 * tileSize));
			localizer.quickLocalization();
			//Move the to y position on the grid line in front of the tunnel
			move.travelTo(odo.getXYT()[0], (tunnelTilePosYOP1 - 0.5)*tileSize, false);
			// correct odometer
			localizer.quickLocalization();
			//Move the to y position on the grid line in the middle of the tile in front of the tunnel
            move.travelTo(odo.getXYT()[0], tunnelTilePosYOP1*tileSize, false);
            // finally face the tunnel entrance
			move.turnTo(turnToTunnel);
			localizer.quickLocalization(); //Make sure we are well facing the tunnel
		}
		else { //Path 2 to tunnel depending on position: if tunnel is on the north or south side of the starting zone
			// move to y position on the grid line before the tunnel
		    move.travelTo(odo.getXYT()[0], tunnelTilePosYOP2*tileSize, false);
			localizer.quickLocalization();
			move.driveDistance(-lightSensorToWheelbase);
			// turn to the x position between gridlines before the tunnel
			move.turnTo(move.calculateAngle(odo.getXYT()[0], odo.getXYT()[1], tunnelTilePosXOP2*tileSize, odo.getXYT()[1]));
            localizer.quickLocalization();
            //Move the to x position on the grid line in front of the tunnel
			move.travelTo(tunnelTilePosXOP2*tileSize, odo.getXYT()[1], false);
			// finally face the tunnel entrance
			move.turnTo(turnToTunnel);
			localizer.quickLocalization();
		}
	}
	
	
	/**
	 * Travel across the tunnel from front to back or from back to front
	 * @param direction Boolean: if true, the robot is going from starting zone to search zone, if false, the robot is going from search zone to starting zone
	 */
	public void throughTunnel(boolean direction) {
		int posCorX = 0, posCorY = 0;
		int thetaCor = 0;
		boolean turnLoc = true;
		if (direction) { //If robot is going from starting zone to search zone, these are the parameters to use
			switch(SC) {
			case 0:
				if(TURX > STZURX) {
					if(TURY == IURY) {
						turnLoc = true;
						posCorX = TURX + 1;
						posCorY = TURY - 1;
					} else {
						turnLoc = false;
						posCorX = TURX + 1;
						posCorY = TURY;
					}
				} else {
					if(TURX == IURX) {
						turnLoc = false;
						posCorX = TURX - 1;
						posCorY = TURY + 1;
					} else {
						turnLoc = true;
						posCorX = TURX;
						posCorY = TURY + 1;
					}
				}
				break;
			case 1:
				if(TLLX < STZLLX) {
					if(TURY == IURY) {
						turnLoc = false;
						posCorX = TLLX - 1;
						posCorY = TLLY;
					} else {
						turnLoc = true;
						posCorX = TLLX - 1;
						posCorY = TLLY + 1;
					}
				} else { 
					if(TLLX == ILLX) {
						turnLoc = true; 
						posCorX = TURX;
						posCorY = TURY + 1;
					} else {
						turnLoc = false;
						posCorX = TURX - 1;
						posCorY = TURY + 1;
					}
					
				}
				break;
			case 2:
				if(TLLX < STZLLX) {
					if(TLLY == ILLY) {
						turnLoc = true; 
						posCorX = TLLX - 1;
						posCorY = TLLY + 1;
					} else {
						turnLoc = false;
						posCorX = TLLX - 1;
						posCorY = TLLY;
					}
				} else {
					if(TLLX == ILLX) {
						turnLoc = false;
						posCorX = TLLX + 1;
						posCorY = TLLY - 1;
					} else {
						turnLoc = true;
						posCorX = TLLX;
						posCorY = TLLY - 1;
					}
				}
				break;
			case 3:
				if(TURX > STZURX) {
					if(TLLY == ILLY) {
						turnLoc = false;
						posCorX = TURX + 1;
						posCorY = TURY;
					} else {
						turnLoc = true;
						posCorX = TURX + 1;
						posCorY = TURY - 1;
					}
				} else {
					if(TLLX == ILLX) {
						turnLoc = false;
						posCorX = TLLX + 1;
						posCorY = TLLY - 1;
					} else {
						turnLoc = true;
						posCorX = TLLX;
						posCorY = TLLY - 1;
					}
				}
				break;
				default:
					break;
			}
		} else { //If robot is going from search zone to starting zone, these are the parameters to use
			switch(SC) {
			case 0:
				if(TURX > STZURX) {
					if(TLLY == STZLLY) {
						turnLoc = true;
						posCorX = TLLX - 1;
						posCorY = TLLY + 1;
					} else {
						turnLoc = false;
						posCorX = TLLX - 1;
						posCorY = TLLY;
					}
				} else {
					if(TLLX == STZLLX) {
						turnLoc = false;
						posCorX = TLLX + 1;
						posCorY = TLLY - 1;
					} else {
						turnLoc = true;
						posCorX = TLLX;
						posCorY = TURY - 1;
					}
				}
				break;
			case 1:
				if(TLLX < STZLLX) {
					if(TLLY == ILLY) {
						turnLoc = false;
						posCorX = TURX + 1;
						posCorY = TURY;
					} else {
						turnLoc = true;
						posCorX = TURX + 1;
						posCorY = TLLY - 1;
					}
				} else { 
					if(TURX == IURX) {
						turnLoc = true; 
						posCorX = TLLX;
						posCorY = TLLY - 1;
					} else {
						turnLoc = false;
						posCorX = TLLX + 1;
						posCorY = TLLY - 1;
					}
					
				}
				break;
			case 2:
				if(TLLX < STZLLX) {
					if(TURY == STZURY) {
						turnLoc = true; 
						posCorX = TURX + 1;
						posCorY = TURY - 1;
					} else {
						turnLoc = false;
						posCorX = TURX + 1;
						posCorY = TURY;
					}
				} else {
					if(TURX == STZURX) {
						turnLoc = false;
						posCorX = TURX - 1;
						posCorY = TURY + 1;
					} else {
						turnLoc = true;
						posCorX = TURX;
						posCorY = TURY + 1;
					}
				}
				break;
			case 3:
				if(TURX > STZURX) {
					if(TURY == STZURY) {
						turnLoc = false;
						posCorX = TLLX - 1;
						posCorY = TLLY;
					} else {
						turnLoc = true;
						posCorX = TLLX - 1;
						posCorY = TLLY + 1;
					}
				} else {
					if(TLLX == STZLLX) {
						turnLoc = true;
						posCorX = TURX;
						posCorY = TLLY + 1;
					} else {
						turnLoc = false;
						posCorX = TURX - 1;
						posCorY = TURY + 1;
					}
				}
				break;
				default:
					break;
			}
		}
		
		
		
		move.driveDistance((bridgeTileLength+2)*tileSize - lightSensorToWheelbase); //Cross tunnel

		localizer.quickThetaCorrection(); //Correct angle and x position 
		move.driveDistance(-lightSensorToWheelbase); 
		move.rotateAngle(90, turnLoc);

		localizer.quickThetaCorrection(); //Correct y position
		move.driveDistance(-lightSensorToWheelbase);
		
		thetaCor = move.roundAngle(); //Update the odometer
		odo.setXYT(posCorX*tileSize, posCorY*tileSize, thetaCor);
		
	}
	
    public void goToStartingTile() {

        // if sc is 0, we want to go to (1,1)
        if (SC == 0) {
            move.travelTo(tileSize, tileSize, false);

        }
        // if SC is 1, we want to go to (14,1)
        else if (SC == 1) {
            move.travelTo(14 * tileSize, tileSize, false);
        }
        // if SC =, we want to go to (14,8)
        else if (SC == 2) {
            move.travelTo(14 * tileSize, 8 * tileSize, false);
        }
        // if SC = 3, we want to go to (1,8)
        else {
            move.travelTo(tileSize, 8 * tileSize, false);
        }
    }

    /**
     *  sets the dumping waypoint depending on the tunnel and startingzone
     *  
     *  @author Alice Kazarine
     */
    public void goToDumpPoint() {
    	//TODO change the spot because the scanning points are in the way of the dumping
       
        if (SC == 0 || SC == 3) {
        	move.turnTo(270);
        	move.driveDistance(tileSize, false);
        	
        }
        else {
        	move.turnTo(90);
        	move.driveDistance(tileSize, false);
        	
        }
    }

    //TODO: this is setup for the beta demo, it should be changed after the demo for the actual project
    // for the demo: it beeps 10 times then travels to the upper right of the search zone
    public void travelBackToStartingCorner() {
        Beeper.foundCan();
        travelToSearchZoneUR();
        Beeper.arrivedAtSearchUR();
        System.exit(0);
    }
}
