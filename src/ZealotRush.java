import java.awt.List;
import java.util.ArrayList;
import java.util.HashSet;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;

public class ZealotRush {

	public HashSet<Position> enemyBuildingMemory = new HashSet<Position>();

	private Game game;
	private Unit mainBase;
	private Unit mainBaseBuilder;
	private Unit scout;
	private Unit target;
	private boolean scouting = false;
	private ArrayList<Unit> zealots;
	private boolean attacking = false;

	public ZealotRush(Game game) {
		this.game = game;
		this.zealots = new ArrayList<Unit>();
	}

	public void strategy(Unit unit, Player player, int numPylons) {
		if (unit.getType() == UnitType.Protoss_Zealot
				&& !zealots.contains(unit)) {
			zealots.add(unit);
			System.out.println("Zealots size is " + zealots.size());
		}
		
		if(unit.getType() == UnitType.Protoss_Gateway){
			unit.setRallyPoint(BWTA.getChokepoints().get(0).getPoint());
		}
		
		if (mainBase == null && unit.getType() == UnitType.Protoss_Nexus) {
			System.out.println("Setting main base");
			mainBase = unit;
		}

		if (mainBaseBuilder == null && unit.getType() == UnitType.Protoss_Probe
				&& mainBase != null) {
			mainBaseBuilder = unit;
			mainBaseBuilder.distanceTo(mainBase.getX(), mainBase.getY());
			System.out.println("Main worker: " + mainBaseBuilder);
		}

		if (scout == null && unit.getType() == UnitType.Protoss_Probe && unit != mainBaseBuilder
				&& player.allUnitCount(UnitType.Protoss_Pylon) > (1+numPylons)) {
			scout = unit;
			System.out.println("Scout: " + scout);
		}

		// Build initial 6 workers
		if (unit.getType() == UnitType.Protoss_Nexus && player.minerals() >= 50
				&& player.allUnitCount(UnitType.Protoss_Probe) < 6) {
			unit.train(UnitType.Protoss_Probe);
			// System.out.println("Workers: " +
			// player.allUnitCount(UnitType.Protoss_Probe));
		}

		// Build first pylon with 6 workers
		else if (player.minerals() >= UnitType.Protoss_Pylon.mineralPrice()
				&& player.allUnitCount(UnitType.Protoss_Pylon) < (1+numPylons)) {
			mainBaseBuilder
					.build(getBuildTile(mainBaseBuilder,
							UnitType.Protoss_Pylon, mainBase.getTilePosition()),
							UnitType.Protoss_Pylon);
			// System.out.println("Main base location: " +
			// mainBase.getTilePosition());
		}

		// Build 2 additional workers after constructing first pylon
		else if (unit.getType() == UnitType.Protoss_Nexus
				&& player.minerals() >= 50
				&& player.allUnitCount(UnitType.Protoss_Probe) < 8
				&& player.allUnitCount(UnitType.Protoss_Pylon) == (1+numPylons)) {
			unit.train(UnitType.Protoss_Probe);
			System.out.println("Workers: "
					+ player.allUnitCount(UnitType.Protoss_Probe));
		}

		// Build a gateway when you have enough minerals
		else if (player.allUnitCount(UnitType.Protoss_Gateway) < 2
				&& player.minerals() >= UnitType.Protoss_Gateway.mineralPrice()
				&& player.allUnitCount(UnitType.Protoss_Pylon) == (1+numPylons)) {
			mainBaseBuilder.build(
					getBuildTile(mainBaseBuilder, UnitType.Protoss_Gateway,
							mainBase.getTilePosition()),
					UnitType.Protoss_Gateway);
			// System.out.println("Main base location: " +
			// mainBase.getTilePosition());
		}

		// Build 2 additional workers after constructing first gateway
		else if (unit.getType() == UnitType.Protoss_Nexus
				&& player.minerals() >= 50
				&& player.allUnitCount(UnitType.Protoss_Probe) < 10
				&& player.allUnitCount(UnitType.Protoss_Gateway) == 1) {
			unit.train(UnitType.Protoss_Probe);
			System.out.println("Workers: "
					+ player.allUnitCount(UnitType.Protoss_Probe));
		}

		else if (player.allUnitCount(UnitType.Protoss_Gateway) < 2
				&& player.minerals() >= UnitType.Protoss_Gateway.mineralPrice()
				&& player.allUnitCount(UnitType.Protoss_Pylon) == (1+numPylons)) {
			mainBaseBuilder.build(
					getBuildTile(mainBaseBuilder, UnitType.Protoss_Gateway,
							mainBase.getTilePosition()),
					UnitType.Protoss_Gateway);
			// System.out.println("Main base location: " +
			// mainBase.getTilePosition());
		}

		// Build second pylon with 2 gateways
		else if (player.minerals() >= UnitType.Protoss_Pylon.mineralPrice()
				&& player.allUnitCount(UnitType.Protoss_Gateway) == 2
				&& player.allUnitCount(UnitType.Protoss_Pylon) == (1+numPylons)) {
			mainBaseBuilder
					.build(getBuildTile(mainBaseBuilder,
							UnitType.Protoss_Pylon, mainBase.getTilePosition()),
							UnitType.Protoss_Pylon);
			// System.out.println("Main base location: " +
			// mainBase.getTilePosition());
		}

		if (player.supplyUsed() / 2 >= (player.supplyTotal() / 2) - 3 && player.allUnitCount(UnitType.Protoss_Gateway) >= 2) {
			if (unit.getType() == UnitType.Protoss_Probe
					&& player.minerals() >= UnitType.Protoss_Pylon.mineralPrice()
					&& player.incompleteUnitCount(UnitType.Protoss_Pylon) == 0) {
				mainBaseBuilder.build( getBuildTile(mainBaseBuilder, UnitType.Protoss_Pylon, mainBase.getTilePosition()), UnitType.Protoss_Pylon);
			}
		}

		if (player.allUnitCount(UnitType.Protoss_Pylon) >= 2
				&& unit.getType() == UnitType.Protoss_Gateway
				&& player.minerals() >= UnitType.Protoss_Zealot.mineralPrice()) {
			if (!unit.isTraining()) {
				unit.train(UnitType.Protoss_Zealot);
			}
		}
		
		if(zealots.size() >= 5 && target != null){
			for(Unit z : zealots){
				z.attack(target);
			}
		}
		
      //if it's a worker and it's idle, send it to the closest mineral patch
      if (unit.getType().isWorker() && unit.isIdle()) {
          Unit closestMineral = null;

          //find the closest mineral
          for (Unit neutralUnit : game.neutral().getUnits()) {
              if (neutralUnit.getType().isMineralField()) {
                  if (closestMineral == null || unit.getDistance(neutralUnit) < unit.getDistance(closestMineral)) {
                      closestMineral = neutralUnit;
                  }
              }
          }
          //if a mineral patch was found, send the drone to gather it
          if (closestMineral != null) {
        	  unit.gather(closestMineral, false);
          }
      }

	}

	public void enemyBase(Player player) {
		if (scout != null) {
			for (BaseLocation b : BWTA.getBaseLocations()) {
				// If this is a possible start location,
				if (b.isStartLocation() && b.getPosition().getApproxDistance(mainBase.getPoint()) > 20) {
					scout.move(b.getPosition());
				}
			}
		}

		// always loop over all currently visible enemy units (even though this
		// set is usually empty)
		for (Unit u : game.enemy().getUnits()) {
			// if this unit is in fact a building
			if (u.getType().isBuilding()) {
				// check if we have it's position in memory and add it if we
				// don't
				if (!enemyBuildingMemory.contains(u.getPosition()))
					enemyBuildingMemory.add(u.getPosition());
			}
			if (u.getType().isWorker() && target == null) {
				target = u;
				System.out.println("Target is " + target.getType());
			}
		}

		// loop over all the positions that we remember
		for (Position p : enemyBuildingMemory) {
			// compute the TilePosition corresponding to our remembered Position
			// p
			TilePosition tileCorrespondingToP = new TilePosition(p.getX() / 32,
					p.getY() / 32);

			// if that tile is currently visible to us...
			if (game.isVisible(tileCorrespondingToP)) {

				// loop over all the visible enemy buildings and find out if at
				// least
				// one of them is still at that remembered position
				boolean buildingStillThere = false;
				for (Unit u : game.enemy().getUnits()) {
					if ((u.getType().isBuilding()) && (u.getPosition() == p)) {
						buildingStillThere = true;
						break;
					}
				}

				// if there is no more any building, remove that position from
				// our memory
				if (buildingStillThere == false) {
					enemyBuildingMemory.remove(p);
					break;
				}
			}
		}
	}

	// Returns a suitable TilePosition to build a given building type near
	// specified TilePosition aroundTile, or null if not found. (builder
	// parameter is our worker)
	public TilePosition getBuildTile(Unit builder, UnitType buildingType,
			TilePosition aroundTile) {
		TilePosition ret = null;
		int maxDist = 3;
		int stopDist = 40;

		// Refinery, Assimilator, Extractor
		if (buildingType.isRefinery()) {
			for (Unit n : game.neutral().getUnits()) {
				if ((n.getType() == UnitType.Resource_Vespene_Geyser)
						&& (Math.abs(n.getTilePosition().getX()
								- aroundTile.getX()) < stopDist)
						&& (Math.abs(n.getTilePosition().getY()
								- aroundTile.getY()) < stopDist))
					return n.getTilePosition();
			}
		}

		while ((maxDist < stopDist) && (ret == null)) {
			for (int i = aroundTile.getX() - maxDist; i <= aroundTile.getX()
					+ maxDist; i++) {
				for (int j = aroundTile.getY() - maxDist; j <= aroundTile
						.getY() + maxDist; j++) {
					if (game.canBuildHere(builder, new TilePosition(i, j),
							buildingType, false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : game.getAllUnits()) {
							if (u.getID() == builder.getID())
								continue;
							if ((Math.abs(u.getTilePosition().getX() - i) < 4)
									&& (Math.abs(u.getTilePosition().getY() - j) < 4))
								unitsInWay = true;
						}
						if (!unitsInWay) {
							return new TilePosition(i, j);
						}
					}
				}
			}
			maxDist += 2;
		}

		if (ret == null)
			game.printf("Unable to find suitable build position for "
					+ buildingType.toString());
		return ret;
	}
	


}
