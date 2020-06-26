package ctfbot;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.FlagInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;

public class GameState {

	private FlagInfo ourFlag;
	public FlagInfo getOurFlag() {
		return ourFlag;
	}

	public void setOurFlag(FlagInfo ourFlag) {
		this.ourFlag = ourFlag;
	}

	public FlagInfo getEnemyFlag() {
		return enemyFlag;
	}

	public void setEnemyFlag(FlagInfo enemyFlag) {
		this.enemyFlag = enemyFlag;
	}

	private UnrealId ourFlagHolder;
	public UnrealId getOurFlagHolder() {
		return ourFlagHolder;
	}

	public void setOurFlagHolder(UnrealId ourFlagHolder) {
		this.ourFlagHolder = ourFlagHolder;
	}

	private boolean isOurFlagSafe = true;
	
	public boolean isOurFlagSafe() {
		return isOurFlagSafe;
	}

	public void setOurFlagSafe(boolean isOurFlagSafe) {
		this.isOurFlagSafe = isOurFlagSafe;
	}

	private FlagInfo enemyFlag;
	private Location ourFlagLocation;
	private Location enemyFlagLocation;
	private Location currentNavigationTarget;
	private Map<UnrealId, Player> players = new HashMap<UnrealId, Player>();
	private boolean nothingToPick;
	private int possibleItemsCount = 0;
	
	public GameState(CTFBot ctfBot) {
	}
	
	public int getPossibleItemsCounts() {
		return possibleItemsCount;
	}

	public Location getOurFlagLocation() {
		return ourFlagLocation;
	}

	public void setOurFlagLocation(Location ourFlagLocation) {
		this.ourFlagLocation = ourFlagLocation;
	}

	public Location getEnemyFlagLocation() {
		return enemyFlagLocation;
	}

	public void setEnemyFlagLocation(Location enemyFlagLocation) {
		this.enemyFlagLocation = enemyFlagLocation;
	}

	public Location getCurrentNavigationTarget() {
		return currentNavigationTarget;
	}

	public void setCurrentNavigationTarget(Location currentNavigationTarget) {
		this.currentNavigationTarget = currentNavigationTarget;
	}

	public void updatePlayer(Player player) {
		players.put(player.getId(), player);
	}

	public void setNothingToPick(boolean b) {
		this.nothingToPick = b;
	}
	
	public boolean hasNothingToPick() {
		return nothingToPick;
	}

	public void setPossibleItemsCounts(int size) {
		this.possibleItemsCount = size;
	}
}
