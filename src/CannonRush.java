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

public class CannonRush {
	private Unit pylon;
	public int numPylons;

	public void strategy(Unit unit, Player player, Game game, Unit scout,
			Unit builder, Unit mainBase) {

		if (player.allUnitCount(UnitType.Protoss_Pylon) == 1
				&& unit.getType() == UnitType.Protoss_Pylon
				&& player.incompleteUnitCount(UnitType.Protoss_Pylon) >= 0 && pylon == null) {
			pylon = unit;
			System.out.println("New pylon");
		}

		// Build initial 6 workers
		if (unit.getType() == UnitType.Protoss_Nexus && player.minerals() >= 50
				&& player.allUnitCount(UnitType.Protoss_Probe) < 6) {
			unit.train(UnitType.Protoss_Probe);
		}

		// Build first pylon with 6 workers
		else if (player.minerals() >= UnitType.Protoss_Pylon.mineralPrice()
				&& player.allUnitCount(UnitType.Protoss_Pylon) < 1
				&& scout.isIdle()) {
			scout.build(
					getBuildTile(scout, UnitType.Protoss_Pylon,
							scout.getTilePosition(), game),
					UnitType.Protoss_Pylon);
		}

		// Build 2 additional workers after constructing first pylon
		else if (unit.getType() == UnitType.Protoss_Nexus
				&& player.minerals() >= 50
				&& player.allUnitCount(UnitType.Protoss_Probe) < 8
				&& player.allUnitCount(UnitType.Protoss_Pylon) == 1) {
			unit.train(UnitType.Protoss_Probe);
			System.out.println("Workers: "
					+ player.allUnitCount(UnitType.Protoss_Probe));
		}

		else if (pylon != null
				&& player.allUnitCount(UnitType.Protoss_Forge) < 1
				&& player.incompleteUnitCount(UnitType.Protoss_Pylon) == 0) {
			TilePosition tp = getBuildTile(scout, UnitType.Protoss_Forge,
					pylon.getTilePosition(), game);
			if(tp != null){
				scout.build(tp, UnitType.Protoss_Forge);
			}
		}

		else if (player.minerals() >= UnitType.Protoss_Photon_Cannon
				.mineralPrice()
				&& player.allUnitCount(UnitType.Protoss_Pylon) == 1
				&& scout != null) {

			if (scout.isIdle()) {
				TilePosition tp = getBuildTile(scout,
						UnitType.Protoss_Photon_Cannon,
						pylon.getTilePosition(), game);
				if (tp != null)
					scout.build(tp, UnitType.Protoss_Photon_Cannon);
				else if (!scout.isMoving()) {
					System.out.println("Tp is null, moving");
					Position pos = new Position(scout.getX() - 20,
							scout.getY() - 20);
					scout.move(pos);
					tp = getBuildTile(scout, UnitType.Protoss_Pylon,
							scout.getTilePosition(), game);
					if (tp != null)
						scout.build(tp, UnitType.Protoss_Pylon);
				}
			}

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

		if (player.supplyUsed() / 2 >= (player.supplyTotal() / 2) - 3
				&& player.allUnitCount(UnitType.Protoss_Gateway) >= 2) {
			if (unit.getType() == UnitType.Protoss_Probe
					&& player.minerals() >= UnitType.Protoss_Pylon
							.mineralPrice()
					&& player.incompleteUnitCount(UnitType.Protoss_Pylon) == 0) {
				System.out.println("Building pylon due to inactiviy");
				builder.build(
						getBuildTile(builder, UnitType.Protoss_Pylon,
								mainBase.getTilePosition(), game),
						UnitType.Protoss_Pylon);
			}
		}
	}

	// Returns a suitable TilePosition to build a given building type near
	// specified TilePosition aroundTile, or null if not found. (builder
	// parameter is our worker)
	public TilePosition getBuildTile(Unit builder, UnitType buildingType,
			TilePosition aroundTile, Game game) {
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

		if (ret == null) {
			game.printf("Unable to find suitable build position for "
					+ buildingType.toString());
			return null;
		}
		return ret;
	}

}
