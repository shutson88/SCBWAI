import bwapi.*;
import bwta.BWTA;

public class GameController extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;
    private ZealotRush zealotRush;
    private CannonRush cannonRush;
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
        //zealotRush = new ZealotRush(game);
        cannonRush = new CannonRush(game, self);
        game.setLocalSpeed(5);

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");
        for(Player p :game.getPlayers() )
        System.out.println(p.getName());

    }

    @Override
	public void onFrame() {
        game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());     
        

        StringBuilder units = new StringBuilder("My units:\n");
        

        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");
            
            cannonRush.strategy(myUnit, self);

            //if there's enough minerals, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50) {
                myUnit.train(UnitType.Terran_SCV);
            }
            
//            //if it's a worker and it's idle, send it to the closest mineral patch
//            if (myUnit.getType().isWorker() && myUnit.isIdle()) {
//                Unit closestMineral = null;
//
//                //find the closest mineral
//                for (Unit neutralUnit : game.neutral().getUnits()) {
//                    if (neutralUnit.getType().isMineralField()) {
//                        if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
//                            closestMineral = neutralUnit;
//                        }
//                    }
//                }
//                //if a mineral patch was found, send the drone to gather it
//                if (closestMineral != null) {
//                    myUnit.gather(closestMineral, false);
//                }
//            }
        }
        
//        zealotRush.enemyBase(self);
        cannonRush.enemyBase(self);
        //draw my units on screen
        game.drawTextScreen(10, 25, units.toString());
    }
    
 // Returns a suitable TilePosition to build a given building type near 
 // specified TilePosition aroundTile, or null if not found. (builder parameter is our worker)
 public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
 	TilePosition ret = null;
 	int maxDist = 3;
 	int stopDist = 40;
 	
 	// Refinery, Assimilator, Extractor
 	if (buildingType.isRefinery()) {
 		for (Unit n : game.neutral().getUnits()) {
 			if ((n.getType() == UnitType.Resource_Vespene_Geyser) && 
 					( Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist ) &&
 					( Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist )
 					) return n.getTilePosition();
 		}
 	}
 	
 	while ((maxDist < stopDist) && (ret == null)) {
 		for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
 			for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
 				if (game.canBuildHere(builder, new TilePosition(i,j), buildingType, false)) {
 					// units that are blocking the tile
 					boolean unitsInWay = false;
 					for (Unit u : game.getAllUnits()) {
 						if (u.getID() == builder.getID()) continue;
 						if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
 					}
 					if (!unitsInWay) {
 						return new TilePosition(i, j);
 					}
 					// creep for Zerg
 					if (buildingType.requiresCreep()) {
 						boolean creepMissing = false;
 						for (int k=i; k<=i+buildingType.tileWidth(); k++) {
 							for (int l=j; l<=j+buildingType.tileHeight(); l++) {
 								if (!game.hasCreep(k, l)) creepMissing = true;
 								break;
 							}
 						}
 						if (creepMissing) continue; 
 					}
 				}
 			}
 		}
 		maxDist += 2;
 	}
 	
 	if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
 	return ret;
 }

    public static void main(String[] args) {
        new GameController().run();
    }
   
}