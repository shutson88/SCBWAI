import java.util.ArrayList;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwapi.UpgradeType;
import bwta.BWTA;

public class ModifiedZealotRush {
	private ArrayList<Unit> army;
	private int numPylons;

	public ModifiedZealotRush() {
		this.army = new ArrayList<Unit>();
		this.numPylons = 0;
	}

	public void strategy(Unit unit, Player player, Game game,
			Position enemyPos, Unit builder, Unit mainBase) {


		
		for(Unit u : player.getUnits()){
			if (u.getType() == UnitType.Protoss_Zealot
					&& !army.contains(u)) {
				army.add(u);
				System.out.println("Army size is " + army.size());
			}

			if (u.getType() == UnitType.Protoss_Dragoon
					&& !army.contains(u)) {
				army.add(u);
				System.out.println("Army size is " + army.size());
			}
		}

		if (unit.getType() == UnitType.Protoss_Gateway) {
			unit.setRallyPoint(enemyPos);
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
				&& numPylons == 0) {
			builder.build(
					getBuildTile(builder, UnitType.Protoss_Pylon,
							mainBase.getTilePosition(), game),
					UnitType.Protoss_Pylon);
			numPylons++;
			System.out.println("Building pylon line 38");
		}
		
		if (numPylons == 1
				&& player.minerals() >= UnitType.Protoss_Gateway.mineralPrice()
				&& player.incompleteUnitCount(UnitType.Protoss_Pylon) == 0
				&& !builder.isConstructing()
				&& player.allUnitCount(UnitType.Protoss_Gateway) <= 1) {
			System.out.println("Trying to buid gateway");
			TilePosition tp = getBuildTile(builder, UnitType.Protoss_Gateway,
					mainBase.getTilePosition(), game);
			if (tp != null) {
				builder.build(tp, UnitType.Protoss_Gateway);
			}
			else{
				tp = getBuildTile(builder, UnitType.Protoss_Pylon,
						mainBase.getTilePosition(), game);
				if (tp != null) {
					builder.build(tp, UnitType.Protoss_Pylon);
				}
			}
		}

		if (unit.getType() == UnitType.Protoss_Gateway
				&& player.minerals() >= UnitType.Protoss_Dragoon.mineralPrice()
				&& player.gas() >= UnitType.Protoss_Dragoon.gasPrice() && !unit.isTraining()) {
			unit.train(UnitType.Protoss_Dragoon);
		} 		
		if (unit.getType() == UnitType.Protoss_Gateway
				&& player.minerals() >= UnitType.Protoss_Zealot.mineralPrice() && !unit.isTraining() && army.size() < 10) {
			unit.train(UnitType.Protoss_Zealot);
		}
		
		if(player.minerals() >= 400 && unit.getType() == UnitType.Protoss_Forge && player.gas() >= 100){
			unit.upgrade(UpgradeType.Protoss_Ground_Weapons);
		}
		
		if(player.minerals() >= 400 && unit.getType() == UnitType.Protoss_Forge && player.gas() >= 100){
			unit.upgrade(UpgradeType.Protoss_Ground_Armor);
		}

		if (unit.getType() == UnitType.Protoss_Gateway) {
			unit.setRallyPoint(BWTA.getChokepoints().get(0).getPoint());
		}

		// Clear dead zealots
		for (Unit u : army) {
			if (!u.exists()) {
				army.remove(u);
			}
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

		if (player.minerals() >= 300
				&& player.allUnitCount(UnitType.Protoss_Assimilator) == 0) {
			builder.build(
					getBuildTile(builder, UnitType.Protoss_Assimilator,
							mainBase.getTilePosition(), game),
					UnitType.Protoss_Assimilator);

		}

		if (player.minerals() >= 400
				&& player.allUnitCount(UnitType.Protoss_Cybernetics_Core) == 0 && numPylons >= 1 && player.allUnitCount(UnitType.Protoss_Gateway) > 0) {
			TilePosition tp = getBuildTile(builder, UnitType.Protoss_Cybernetics_Core,
					mainBase.getTilePosition(), game);
			if(tp != null)
				builder.build(tp, UnitType.Protoss_Cybernetics_Core);

		}

	}

	private boolean buildingBeingConstructed(UnitType building, Player player) {
		for (Unit u : player.getUnits()) {
			if (u.isBeingConstructed() && u.getType() == building) {
				return true;
			}
		}
		return false;
	}

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

	public ArrayList<Unit> getArmy() {
		return army;
	}

}