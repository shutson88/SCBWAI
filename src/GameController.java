import java.util.ArrayList;
import java.util.HashSet;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class GameController extends DefaultBWListener {

	private Mirror mirror = new Mirror();

	private Game game;
	public Unit mainBaseBuilder;
	public Unit mainBase;
	private Player enemy;
	private Unit scout;
	private Position enemyPos;
	private Player self;
	private TransitionFromCR transition;
	private CannonRush cannonRush;
	private boolean explored = false;
	private boolean attacking = false;
	private boolean isCannonRushing = true;
	private ArrayList<Unit> gasWorkers;
	private Unit refinery;
	public HashSet<Position> enemyBuildingMemory = new HashSet<Position>();

	public void run() {
		mirror.getModule().setEventListener(this);
		mirror.startGame();

	}

	@Override
	public void onUnitCreate(Unit unit) {
		System.out.println("New unit " + unit.getType());
	}

	@Override
	public void onStart() {
		game = mirror.getGame();
		self = game.self();
		transition = new TransitionFromCR();
		cannonRush = new CannonRush();
		game.setLocalSpeed(5);
		gasWorkers = new ArrayList<Unit>();

		for (Player pl : game.getPlayers()) {
			if (pl.isEnemy(self)) {
				enemy = pl;
				System.out.println("Enemy: " + enemy.getName());
			}
		}

		// Use BWTA to analyze map
		// This may take a few minutes if the map is processed first time!
		System.out.println("Analyzing map...");
		BWTA.readMap();
		BWTA.analyze();
		System.out.println("Map data ready");
		for (Player p : game.getPlayers())
			System.out.println(p.getName());

	}

	@Override
	public void onFrame() {
		game.setTextSize(10);
		game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - "
				+ self.getRace());

		StringBuilder units = new StringBuilder("My units:\n");

		// iterate through my units
		for (Unit myUnit : self.getUnits()) {
			units.append(myUnit.getType()).append(" ")
					.append(myUnit.getTilePosition()).append("\n");

			// Maintain access to primary base
			if (mainBase == null && myUnit.getType() == UnitType.Protoss_Nexus) {
				System.out.println("Setting main base");
				mainBase = myUnit;
			}

			// Keep a builder near main base
			if (mainBaseBuilder == null
					&& myUnit.getType() == UnitType.Protoss_Probe
					&& mainBase != null && myUnit != scout) {
				mainBaseBuilder = myUnit;
				mainBaseBuilder.distanceTo(mainBase.getX(), mainBase.getY());
				System.out.println("Main worker: " + mainBaseBuilder);

			}

			// Keep track of unit that is scouting
			if (scout == null && myUnit.getType() == UnitType.Protoss_Probe
					&& myUnit != mainBaseBuilder) {
				scout = myUnit;
				System.out.println("Scout: " + scout);
				Position pos = new Position(enemy.getStartLocation().getX(),
						enemy.getStartLocation().getY());
				scout.move(pos);
				System.out.println("Enemy Location: " + pos);
			}

			// if it's a worker and it's idle, send it to the closest mineral
			// patch
			if (myUnit.getType().isWorker() && myUnit.isIdle()
					&& myUnit != scout) {
				Unit closestMineral = null;

				// find the closest mineral
				for (Unit neutralUnit : game.neutral().getUnits()) {
					if (neutralUnit.getType().isMineralField()) {
						if (closestMineral == null
								|| myUnit.getDistance(neutralUnit) < myUnit
										.getDistance(closestMineral)) {
							closestMineral = neutralUnit;
						}
					}
				}
				// if a mineral patch was found, send the drone to gather it
				if (closestMineral != null) {
					myUnit.gather(closestMineral, false);
				}
			}

			if (myUnit.getType() == UnitType.Protoss_Assimilator
					&& refinery == null) {
				System.out.println("Setting refinery");
				refinery = myUnit;
			}

			if (gasWorkers.size() < 3
					&& myUnit.getType() == UnitType.Protoss_Probe
					&& refinery != null && refinery.isCompleted()) {
				System.out.println("Gather gas");
				myUnit.gather(refinery);
				gasWorkers.add(myUnit);
			}

			if (self.minerals() >= 450 || !scout.exists()) {
				transition.strategy(myUnit, self, game, enemyPos,
						mainBaseBuilder, mainBase);
				isCannonRushing = false;
			} else if (isCannonRushing)
				cannonRush.strategy(myUnit, self, game, scout, mainBaseBuilder,
						mainBase);
		}

		enemyBase(self);

		// draw my units on screen
		game.drawTextScreen(10, 25, units.toString());
	}

	public void enemyBase(Player player) {
		if (scout != null && !attacking) {
			for (BaseLocation b : BWTA.getBaseLocations()) {
				// If this is a possible start location,
				if (b.isStartLocation()
						&& b.getPosition().getApproxDistance(
								mainBase.getPoint()) > 20) {
					int x;
					int y;
					if (mainBase.getY() > b.getPosition().getY()) {
						y = -20;
					} else {
						y = 20;
					}
					if (mainBase.getX() > b.getPosition().getX()) {
						x = 450;
					} else {
						x = -100;
					}
					enemyPos = new Position(b.getPosition().getX() + x, b
							.getPosition().getY() + y);
					scout.move(enemyPos);
					attacking = true;
				}
				/*
				 * if (b.isStartLocation() &&
				 * b.getPosition().getApproxDistance(mainBase.getPoint()) > 20)
				 * { if(scout.getY() > b.getPosition().getY()) enemyPos = new
				 * Position(b.getPosition().getX()-90,
				 * b.getPosition().getY()-40); else enemyPos = new
				 * Position(b.getPosition().getX()+100,
				 * b.getPosition().getY()-150 ); scout.move(enemyPos); attacking
				 * = true; }
				 */
			}
		}

		// always loop over all currently visible enemy units (even though this
		// set is usually empty)
		System.out
				.println("Enemy unit size: " + game.enemy().getUnits().size());
		System.out.println("Army size: " + transition.getArmy().size());
		for (Unit u : game.enemy().getUnits()) {
			// if this unit is in fact a building
			if (u.getType().isBuilding()) {
				// check if we have it's position in memory and add it if we
				// don't
				if (!enemyBuildingMemory.contains(u.getPosition()))
					enemyBuildingMemory.add(u.getPosition());
			}

			for (Unit a : transition.getArmy()) {
				if (a.isIdle() && transition.getArmy().size() >= 10
						&& u.getType() != UnitType.Zerg_Larva) {
					a.attack(u);
				}
			}
		}
		// Go scout
		if (game.enemy().getUnits().size() == 0
				&& transition.getArmy().size() >= 20) {
			for (int b = 0; b < BWTA.getBaseLocations().size(); b++) {
				// If this is a possible start location,
				for (Unit a : transition.getArmy()) {
					if (a.isIdle() && !a.isMoving()) {
						a.move(BWTA.getBaseLocations().get(b).getPosition());
					}
					if (b < BWTA.getBaseLocations().size() - 1) {
						b++;
					}

				}

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
					for (Unit a : transition.getArmy()) {
						if (a.isIdle() && transition.getArmy().size() >= 10
								&& u.getType() != UnitType.Zerg_Larva) {
							a.attack(u);
						}
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
						// creep for Zerg
						if (buildingType.requiresCreep()) {
							boolean creepMissing = false;
							for (int k = i; k <= i + buildingType.tileWidth(); k++) {
								for (int l = j; l <= j
										+ buildingType.tileHeight(); l++) {
									if (!game.hasCreep(k, l))
										creepMissing = true;
									break;
								}
							}
							if (creepMissing)
								continue;
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

	public static void main(String[] args) {
		new GameController().run();
	}

}