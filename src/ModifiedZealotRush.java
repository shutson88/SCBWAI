import java.util.ArrayList;

import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;

public class ModifiedZealotRush {
	private ArrayList<Unit> zealots;	
	private int numPylons;
	public ModifiedZealotRush() {
		this.zealots = new ArrayList<Unit>();
		this.numPylons = 0;
	}

	public void strategy(Unit unit, Player player, Game game, Position enemyPos, Unit builder, Unit mainBase) {
		
		if (unit.getType() == UnitType.Protoss_Zealot
				&& !zealots.contains(unit)) {
			zealots.add(unit);
			System.out.println("Zealots size is " + zealots.size());
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
			builder.build(getBuildTile(builder,
							UnitType.Protoss_Pylon, mainBase.getTilePosition(), game),
							UnitType.Protoss_Pylon);
			numPylons++;
			System.out.println("Building pylon line 38" );
		}
		if (numPylons == 1
				&& player.minerals() >= UnitType.Protoss_Gateway.mineralPrice()
				&& player.incompleteUnitCount(UnitType.Protoss_Pylon) == 0
				&& !builder.isConstructing()
				&& player.allUnitCount(UnitType.Protoss_Gateway) <= 1) {
			System.out.println("Trying to buid gateway");
			TilePosition tp = getBuildTile(builder,
					UnitType.Protoss_Gateway, mainBase.getTilePosition(), game);
			if (tp != null) {
				builder.build(tp, UnitType.Protoss_Gateway);
			}
		}
		if (unit.getType() == UnitType.Protoss_Gateway
				&& player.minerals() >= UnitType.Protoss_Zealot.mineralPrice()) {
			unit.train(UnitType.Protoss_Zealot);
		}
		if (unit.getType() == UnitType.Protoss_Gateway) {
			unit.setRallyPoint(BWTA.getChokepoints().get(0).getPoint());
		}
		
		//Clear dead zealots
		for(Unit z : zealots){
			if(!z.exists()){
				zealots.remove(z);
			}
		}
		
		if (player.supplyUsed() / 2 >= (player.supplyTotal() / 2) - 3 && player.allUnitCount(UnitType.Protoss_Gateway) >= 2) {
			if (unit.getType() == UnitType.Protoss_Probe
					&& player.minerals() >= UnitType.Protoss_Pylon.mineralPrice()
					&& player.incompleteUnitCount(UnitType.Protoss_Pylon) == 0) {
				System.out.println("Building pylon due to inactiviy");
				builder.build( getBuildTile(builder, UnitType.Protoss_Pylon, mainBase.getTilePosition(), game), UnitType.Protoss_Pylon);
			}
		}
		
		if(player.minerals() >= 600 && player.allUnitCount(UnitType.Protoss_Assimilator) == 0){
			builder.build(getBuildTile(builder, UnitType.Protoss_Assimilator,
					mainBase.getTilePosition(), game), UnitType.Protoss_Assimilator);
			
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

			if (ret == null){
				game.printf("Unable to find suitable build position for " + buildingType.toString());
				return null;
			}
			return ret;
		}
		
		public ArrayList<Unit> getZealots() {
			return zealots;
		}
}