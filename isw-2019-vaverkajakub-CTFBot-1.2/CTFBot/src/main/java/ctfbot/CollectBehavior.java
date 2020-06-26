package ctfbot;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.NavPoint;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.utils.Cooldown;

public class CollectBehavior implements IBehavior {


	private Item target = null;
	private CTFBot bot = null;

	public CollectBehavior(CTFBot ctfBot) {
		this.bot = ctfBot;
	}
	
	@Override
	public void run() {
		bot.getShoot().stopShooting();
		if (target == null) {
			if(!pickTarget()) {
				bot.getGameState().setNothingToPick(true);
			}
		}

		if (target != null) {	
			bot.getNavigation().navigate(target);
			if(!bot.getNavigation().isNavigating() || bot.getNavigation().getPathExecutor().getPath() == null) {
				bot.getLog().warning("Target reset");
				target = null;
			}
		}
	}
	

	@Override
	public boolean isFiring() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean terminate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getPriority() {
		Collection<Item> possibleItems = bot.getItems().getSpawnedItems().values().stream().filter((item) -> {
			return bot.getItems().isPickable(item)  && !bot.getForbiddenItems().isTaboo(item);
		}).collect(Collectors.toList());
		if (bot.getGameState().getPossibleItemsCounts()* 0.8
				 <  (possibleItems.size() )) {
			return 110;
		}
		return 0;
	}

	private boolean pickTarget() {
		Item targetItem =
				  DistanceUtils.getNearest(
				    bot.getItems().getSpawnedItems().values().stream().filter(
					    	(item) -> {
					    		return bot.getItems().isPickable(item)  && !bot.getForbiddenItems().isTaboo(item);
					    	}
				        ).collect(Collectors.toList()),
				    bot.getInfo().getLocation(),
				    new DistanceUtils.GetLocatedDistance3D());

		target = targetItem;
		bot.targetItem = targetItem;
		if(target != null) {
			return true;
		}else {
			return false;
		}
	}

	private boolean collectItems(Collection<UT2004ItemType> requiredItems) {
		Set<Item> nearest = getNearestSpawnedItems(requiredItems);

    	Item target = DistanceUtils.getNearest(nearest, bot.getInfo().getNearestNavPoint(), 
			new DistanceUtils.GetLocatedDistance3D<ILocated>());
		if (target == null) {
			bot.getLog().severe("No item to navigate to! requiredWeapons.size() = " + requiredItems.size());
			return false;
		}
		
		bot.getNavigation().navigate(target);
		
		return true;
	}
    
    /**
     * Translates 'types' to the set of "nearest spawned items" of those 'types'.
     * @param types
     * @return
     */
    private Set<Item> getNearestSpawnedItems(Collection<UT2004ItemType> types) {
    	Set<Item> result = new HashSet<Item>();
    	for (UT2004ItemType type : types) {
    		Item n = getNearestSpawnedItem(type);
    		if (n != null) {
    			result.add(n);
    		}
    	}
    	return result;
    }
    
    /**
     * Returns the nearest spawned item of 'type'.
     * @param type
     * @return
     */
    private Item getNearestSpawnedItem(UT2004ItemType type) {
    	final NavPoint nearestNavPoint = bot.getInfo().getNearestNavPoint();
    	Item nearest = DistanceUtils.getNearest(
    			bot.getItems().getSpawnedItems(type).values(), 
    			bot.getInfo().getNearestNavPoint(),
    			new DistanceUtils.GetLocatedDistance3D<ILocated>());
    	return nearest;
    }

   
}
